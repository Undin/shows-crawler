#[macro_use]
extern crate derive_new;
#[macro_use]
extern crate diesel;
#[macro_use]
extern crate diesel_codegen;
extern crate reqwest;
#[macro_use]
extern crate serde_derive;

pub mod models;
pub mod schema;
pub mod telegram_api;
