use reqwest::{Client, Result, Response};
use std::collections::HashMap;

pub struct TelegramApi {
    client: Client,
    base_url: String
}

impl TelegramApi {

    pub fn new<S: Into<String>>(client: Client, token: S) -> TelegramApi {
        TelegramApi { client: client, base_url: format!("https://api.telegram.org/bot{}", token.into()) }
    }

    pub fn get_updates(&self, timeout: u32, limit: u32, offset: i32) -> Result<UpdateResponse> {
        let url = format!("{}/getUpdates?timeout={}&limit={}&offset={}", self.base_url, timeout, limit, offset);
        println!("--> {}", url);
        let result: Result<Response> = self.client.get(&url).send();
        match result {
            Ok(mut response) => response.json(),
            Err(error) => Err(error)
        }
    }

    pub fn send_message(&self, chat_id: i64, text: &str) -> Result<()> {
        let mut params = HashMap::with_capacity(2);
        params.insert("chat_id", chat_id.to_string());
        params.insert("text", text.to_owned());
        let url = format!("{}/sendMessage", self.base_url);
        println!("--> {}", url);
        let response = self.client.post(&url)
            .form(&params)
            .send();
        response.map(|_| ())
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct User {
    pub id: i32,
    pub first_name: String
}

#[derive(Serialize, Deserialize, Debug)]
pub enum ChatType {
    #[serde(rename = "private")]
    Private,
    #[serde(rename = "group")]
    Group,
    #[serde(rename = "supergroup")]
    Supergroup,
    #[serde(rename = "channel")]
    Channel
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Chat {
    pub id: i64,
    #[serde(rename = "type")]
    pub chat_type: ChatType
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Message {
    pub message_id: i32,
    pub chat: Chat,
    pub date: u64,
    pub from: Option<User>,
    pub text: Option<String>
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Update {
    pub update_id: i32,
    pub message: Option<Message>
}

#[derive(Serialize, Deserialize, Debug)]
pub struct UpdateResponse {
    pub result: Vec<Update>
}
