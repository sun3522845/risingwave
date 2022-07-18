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

use std::assert_matches::assert_matches;

use itertools::Itertools;
use risingwave_common::catalog::Field;
use risingwave_common::error::ErrorCode;
use risingwave_common::types::{unnested_list_type, DataType};
use risingwave_sqlparser::ast::FunctionArg;

use super::{Binder, Result};
use crate::binder::table_function::{BoundTableFunction, TableFunctionType};
use crate::expr::{Expr as _, ExprImpl, ExprType};

impl Binder {
    pub(super) fn bind_unnest_function(
        &mut self,
        args: Vec<FunctionArg>,
    ) -> Result<BoundTableFunction> {
        // unnest ( Array[...] )
        if args.len() != 1 {
            return Err(ErrorCode::BindError(
                "the length of args of unnest function should be 1".to_string(),
            )
            .into());
        }

        let exprs = self.bind_function_arg(args[0].clone())?;
        if exprs.len() != 1 {
            return Err(ErrorCode::BindError(
                "the length of expr of unnest function should be 1".to_string(),
            )
            .into());
        }

        let expr = exprs.into_iter().next().unwrap();
        if let ExprImpl::FunctionCall(ref func) = expr {
            if func.get_expr_type() == ExprType::Array {
                let list_type = func.return_type();
                assert_matches!(list_type, DataType::List { datatype: _ },);
                let data_type = unnested_list_type(list_type);

                let columns = [(
                    false,
                    Field {
                        data_type: data_type.clone(),
                        name: "unnest".to_string(),
                        sub_fields: vec![],
                        type_name: "".to_string(),
                    },
                )]
                .into_iter();

                self.bind_context(columns, "unnest".to_string(), None)?;

                Ok(BoundTableFunction {
                    args: vec![expr],
                    return_type: data_type,
                    function_type: TableFunctionType::Unnest,
                })
            } else {
                Err(ErrorCode::BindError(
                    "the expr function of unnest function should be array".to_string(),
                )
                .into())
            }
        } else {
            unimplemented!()
        }
    }

    pub(super) fn bind_generate_series_function(
        &mut self,
        args: Vec<FunctionArg>,
    ) -> Result<BoundTableFunction> {
        let args = args.into_iter();

        // generate_series ( start timestamp, stop timestamp, step interval ) or
        // generate_series ( start i32, stop i32, step i32 )
        if args.len() != 3 {
            return Err(ErrorCode::BindError(
                "the length of args of generate series function should be 3".to_string(),
            )
            .into());
        }

        let exprs: Vec<_> = args
            .map(|arg| self.bind_function_arg(arg))
            .flatten_ok()
            .try_collect()?;

        let data_type = type_check(&exprs)?;

        let columns = [(
            false,
            Field {
                data_type: data_type.clone(),
                name: "generate_series".to_string(),
                sub_fields: vec![],
                type_name: "".to_string(),
            },
        )]
        .into_iter();

        self.bind_context(columns, "generate_series".to_string(), None)?;

        Ok(BoundTableFunction {
            args: exprs,
            return_type: data_type,
            function_type: TableFunctionType::Generate,
        })
    }
}

fn type_check(exprs: &[ExprImpl]) -> Result<DataType> {
    let mut exprs = exprs.iter();
    let Some((start, stop,step)) = exprs.next_tuple() else {
        return Err(ErrorCode::BindError("Invalid arguments for Generate series function".to_string()).into())
    };
    match (start.return_type(), stop.return_type(), step.return_type()) {
        (DataType::Int32, DataType::Int32, DataType::Int32) => Ok(DataType::Int32),
        (DataType::Timestamp, DataType::Timestamp, DataType::Interval) => Ok(DataType::Timestamp),
        _ => Err(ErrorCode::BindError(
            "Invalid arguments for Generate series function".to_string(),
        )
        .into()),
    }
}
