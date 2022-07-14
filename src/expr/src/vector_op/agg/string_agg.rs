// Copyright 2022 Singularity Data
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::cmp::Ordering;
use std::collections::BinaryHeap;
use std::sync::Arc;

use itertools::Itertools;
use risingwave_common::array::{Array, ArrayBuilder, ArrayBuilderImpl, ArrayImpl, DataChunk, Row};
use risingwave_common::error::{ErrorCode, Result};
use risingwave_common::types::{DataType, ScalarImpl};
use risingwave_common::util::sort_util::{compare_rows, OrderPair};

use crate::vector_op::agg::aggregator::Aggregator;

#[derive(Debug, Clone, PartialEq, Eq)]
struct OrderableRow {
    row: Row,
    order_pairs: Arc<Vec<OrderPair>>,
}

impl Ord for OrderableRow {
    fn cmp(&self, other: &Self) -> Ordering {
        let ord = compare_rows(&self.order_pairs, &self.row, &other.row).unwrap();
        ord.reverse() // we have to reverse the order because BinaryHeap is a max-heap
    }
}

impl PartialOrd for OrderableRow {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

pub struct StringAgg {
    agg_col_idx: usize,
    order_pairs: Arc<Vec<OrderPair>>,
    min_heap: BinaryHeap<OrderableRow>,
    all_nulls: bool,
}

impl StringAgg {
    pub fn new(agg_col_idx: usize, order_pairs: Vec<OrderPair>) -> Self {
        StringAgg {
            agg_col_idx,
            order_pairs: Arc::new(order_pairs),
            min_heap: BinaryHeap::new(),
            all_nulls: true,
        }
    }

    fn push_row(&mut self, s: &str, chunk: &DataChunk, row_id: usize) -> Result<()> {
        // TODO(rc): for agg w/o order by clause, just aggregate the string on the fly
        let (row_ref, vis) = chunk.row_at(row_id)?;
        assert!(vis);
        let row = row_ref.to_owned_row();
        // TODO(rc): save string instead of ScalarImpl
        self.min_heap.push(OrderableRow {
            row,
            order_pairs: self.order_pairs.clone(),
        });
        self.all_nulls = false; // once a `s: &str` is given, we know that not all rows are nulls
        Ok(())
    }

    fn get_result(&self) -> Option<String> {
        if self.all_nulls {
            None
        } else {
            // TODO(yuchao): support delimiter
            Some(
                self.min_heap
                    .clone()
                    .into_iter_sorted()
                    .map(
                        |orow| match orow.row.into_value_at(self.agg_col_idx).unwrap() {
                            ScalarImpl::Utf8(s) => s,
                            _ => panic!("Expected Utf8"),
                        },
                    )
                    .join(""),
            )
        }
    }

    fn get_result_and_reset(&mut self) -> Option<String> {
        if self.all_nulls {
            None
        } else {
            self.all_nulls = true;
            // TODO(yuchao): support delimiter
            Some(
                self.min_heap
                    .drain_sorted()
                    .map(
                        |orow| match orow.row.into_value_at(self.agg_col_idx).unwrap() {
                            ScalarImpl::Utf8(s) => s,
                            _ => panic!("Expected Utf8"),
                        },
                    )
                    .join(""),
            )
        }
    }
}

impl Aggregator for StringAgg {
    fn return_type(&self) -> DataType {
        DataType::Varchar
    }

    fn update_single(&mut self, input: &DataChunk, row_id: usize) -> Result<()> {
        log::warn!("[rc] update_single, input: {:?} row_id: {}", input, row_id);
        if let ArrayImpl::Utf8(col) = input.column_at(self.agg_col_idx).array_ref() {
            if let Some(s) = col.value_at(row_id) {
                // only need to save rows with non-empty string value to aggregate
                self.push_row(s, input, row_id)?;
            }
            Ok(())
        } else {
            Err(
                ErrorCode::InternalError(format!("Input fail to match {}.", stringify!(Utf8)))
                    .into(),
            )
        }
    }

    fn update_multi(
        &mut self,
        input: &DataChunk,
        start_row_id: usize,
        end_row_id: usize,
    ) -> Result<()> {
        log::warn!(
            "[rc] update_multi, input: {:?}, start_row_id: {}, end_row_id: {}",
            input,
            start_row_id,
            end_row_id
        );
        if let ArrayImpl::Utf8(col) = input.column_at(self.agg_col_idx).array_ref() {
            for (i, s) in col
                .iter()
                .enumerate()
                .skip(start_row_id)
                .take(end_row_id - start_row_id)
                .filter(|(_, s)| s.is_some())
            {
                self.push_row(s.unwrap(), input, i)?;
            }
            Ok(())
        } else {
            Err(
                ErrorCode::InternalError(format!("Input fail to match {}.", stringify!(Utf8)))
                    .into(),
            )
        }
    }

    fn output(&self, builder: &mut ArrayBuilderImpl) -> Result<()> {
        if let ArrayBuilderImpl::Utf8(builder) = builder {
            let s = self.get_result();
            if let Some(s) = s {
                builder.append(Some(&s))
            } else {
                builder.append(None)
            }
            .map_err(Into::into)
        } else {
            Err(
                ErrorCode::InternalError(format!("Builder fail to match {}.", stringify!(Utf8)))
                    .into(),
            )
        }
    }

    fn output_and_reset(&mut self, builder: &mut ArrayBuilderImpl) -> Result<()> {
        if let ArrayBuilderImpl::Utf8(builder) = builder {
            let s = self.get_result_and_reset();
            if let Some(s) = s {
                builder.append(Some(&s))
            } else {
                builder.append(None)
            }
            .map_err(Into::into)
        } else {
            Err(
                ErrorCode::InternalError(format!("Builder fail to match {}.", stringify!(Utf8)))
                    .into(),
            )
        }
    }
}
