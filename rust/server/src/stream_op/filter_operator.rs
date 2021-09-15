use super::{Message, Op, Output, StreamChunk, StreamOperator, UnaryStreamOperator};
use crate::{
    array2::{Array, ArrayImpl, DataChunk},
    buffer::Bitmap,
    error::Result,
    expr::BoxedExpression,
};
use async_trait::async_trait;

/// `FilterOperator` filters data with the `expr`. The `expr` takes a chunk of data,
/// and returns a boolean array on whether each item should be retained. And then,
/// `FilterOperator` will insert, delete or update element into next operator according
/// to the result of the expression.
pub struct FilterOperator {
    /// The output of the current operator
    output: Box<dyn Output>,
    /// Expression of the current filter, note that the filter must always have the same output for the same input.
    expr: BoxedExpression,
}

impl FilterOperator {
    pub fn new(output: Box<dyn Output>, expr: BoxedExpression) -> Self {
        Self { output, expr }
    }
}

use crate::impl_consume_barrier_default;

impl_consume_barrier_default!(FilterOperator, StreamOperator);

#[async_trait]
impl UnaryStreamOperator for FilterOperator {
    async fn consume_chunk(&mut self, chunk: StreamChunk) -> Result<()> {
        let StreamChunk {
            ops,
            columns: arrays,
            visibility,
            cardinality,
        } = chunk;

        let data_chunk = {
            let data_chunk_builder = DataChunk::builder()
                .columns(arrays)
                .cardinality(cardinality);
            if let Some(visibility) = visibility {
                data_chunk_builder.visibility(visibility).build()
            } else {
                data_chunk_builder.build()
            }
        };

        // FIXME: unnecessary compact.
        // See https://github.com/singularity-data/risingwave/issues/704
        let data_chunk = data_chunk.compact()?;

        let pred_output = self.expr.eval(&data_chunk)?;

        let (arrays, visibility) = data_chunk.destruct();

        let n = ops.len();

        // TODO: Can we update ops and visibility inplace?
        let mut new_ops = Vec::with_capacity(n);
        let mut new_visibility = Vec::with_capacity(n);
        let mut new_cardinality = 0usize;
        let mut last_res = false;

        assert!(match visibility {
            None => true,
            Some(ref m) => m.len() == n,
        });
        assert!(matches!(&*pred_output, ArrayImpl::Bool(_)));

        if let ArrayImpl::Bool(bool_array) = &*pred_output {
            for (op, res) in ops.into_iter().zip(bool_array.iter()) {
                // SAFETY: ops.len() == pred_output.len() == visibility.len()
                let res = res.unwrap_or(false);
                match op {
                    Op::Insert | Op::Delete => {
                        new_ops.push(op);
                        if res {
                            new_visibility.push(true);
                            new_cardinality += 1;
                        } else {
                            new_visibility.push(false);
                        }
                    }
                    Op::UpdateDelete => {
                        last_res = res;
                    }
                    Op::UpdateInsert => match (last_res, res) {
                        (true, false) => {
                            new_ops.push(Op::Delete);
                            new_ops.push(Op::UpdateInsert);
                            new_visibility.push(true);
                            new_visibility.push(false);
                            new_cardinality += 1;
                        }
                        (false, true) => {
                            new_ops.push(Op::UpdateDelete);
                            new_ops.push(Op::Insert);
                            new_visibility.push(false);
                            new_visibility.push(true);
                            new_cardinality += 1;
                        }
                        (true, true) => {
                            new_ops.push(Op::UpdateDelete);
                            new_ops.push(Op::UpdateInsert);
                            new_visibility.push(true);
                            new_visibility.push(true);
                            new_cardinality += 2;
                        }
                        (false, false) => {
                            new_ops.push(Op::Insert);
                            new_ops.push(Op::Insert);
                            new_visibility.push(false);
                            new_visibility.push(false);
                        }
                    },
                }
            }
        } else {
            panic!("unmatched type: filter expr returns a non-null array");
        }

        let new_chunk = StreamChunk {
            columns: arrays,
            visibility: Some(Bitmap::from_vec(new_visibility)?),
            cardinality: new_cardinality,
            ops: new_ops,
        };

        self.output.collect(Message::Chunk(new_chunk)).await
    }
}
