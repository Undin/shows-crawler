#[macro_use]
extern crate derive_new;
#[macro_use]
extern crate diesel;
#[macro_use]
extern crate diesel_codegen;
extern crate itertools;
#[macro_use]
extern crate log;
extern crate r2d2;
extern crate r2d2_diesel;
extern crate reqwest;
#[macro_use]
extern crate serde_derive;

pub mod commands;
pub mod models;
pub mod schema;
pub mod telegram_api;

use diesel::pg::PgConnection;
use r2d2::{Config, Pool, PooledConnection};
use r2d2_diesel::ConnectionManager;
use reqwest::Client;
use std::sync::Arc;
use telegram_api::{ApiResponse, TelegramApi};

#[derive(Clone)]
pub struct Components {
    pub api: Arc<TelegramApi>,
    pub connection_pool: Pool<ConnectionManager<PgConnection>>
}

impl Components {
    pub fn new<S1: Into<String>, S2: Into<String>>(bot_token: S1, database_url: S2) -> Self {
        let client = Client::new().unwrap();
        let api = Arc::new(TelegramApi::new(client, bot_token));

        let config = Config::default();
        let connection_manager = ConnectionManager::new(database_url);
        let connection_pool = Pool::new(config, connection_manager)
            .expect("Failed to create connection pool");

        Components { api: api, connection_pool: connection_pool}
    }

    pub fn get_connection(&self) -> PooledConnection<ConnectionManager<PgConnection>> {
        self.connection_pool.get()
            .expect("Unable get connection from connection pool")
    }

    pub fn send_message(&self, chat_id: i64, text: &str) {
        self.send_message_internal(chat_id, text, None);
    }

    pub fn send_message_with_buttons(&self, chat_id: i64, text: &str, buttons_info: &[(String, i64)]) {
        self.send_message_internal(chat_id, text, Some(buttons_info));
    }

    fn send_message_internal(&self, chat_id: i64, text: &str, buttons_info: Option<&[(String, i64)]>) {
        debug!("send message. chat_id: {}, message: {}, buttons_info: {:?}", chat_id, text, buttons_info);
        match self.api.send_message(chat_id, text, buttons_info) {
            Ok(ApiResponse { ok: true, .. }) => {},
            Ok(ApiResponse { ok: false, error_code: Some(code), description: desc }) => {
                match desc {
                    Some(desc) => error!("Error code: {}, description: {}", code, desc),
                    None => error!("Error code: {}", code)
                }
            }
            Ok(ApiResponse { ok: false, .. }) => error!("Unknown error"),
            Err(error) => error!("Can't send message: {}", error)
        }
    }
}
