use reqwest::{Client, Result};

pub struct TelegramApi {
    client: Client,
    base_url: String,
    parse_mode: String
}

impl TelegramApi {

    pub fn new<S: Into<String>>(client: Client, token: S) -> TelegramApi {
        TelegramApi {
            client: client,
            base_url: format!("https://api.telegram.org/bot{}", token.into()),
            parse_mode: "Markdown".to_string()
        }
    }

    pub fn get_updates(&self, timeout: u32, limit: u32, offset: i32) -> Result<UpdateResponse> {
        let url = format!("{}/getUpdates?timeout={}&limit={}&offset={}", self.base_url, timeout, limit, offset);
        self.client.get(&url)
            .send()
            .and_then(|mut response| response.json())
    }

    pub fn send_message(&self, chat_id: i64, text: &str, buttons_info: Option<&[(String, i64)]>) -> Result<ApiResponse> {
        let data = match buttons_info {
            Some(info) => {
                let buttons = info.iter()
                    .map(|&(ref label, data)| vec![InlineKeyboardButton::new(label, data.to_string())])
                    .collect::<Vec<_>>();
                SendMessageData::new(chat_id, text, &self.parse_mode, Some(InlineKeyboardMarkup::new(buttons)))
            },
            None => SendMessageData::new(chat_id, text, &self.parse_mode, None)
        };
        self.client.post(&format!("{}/sendMessage", self.base_url))
            .json(&data)
            .send()
            .and_then(|mut response| response.json())
    }
}

#[derive(new, Serialize, Debug)]
pub struct SendMessageData<'a> {
    pub chat_id: i64,
    pub text: &'a str,
    pub parse_mode: &'a str,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reply_markup: Option<InlineKeyboardMarkup<'a>>
}

#[derive(new, Serialize, Debug)]
pub struct InlineKeyboardButton<'a> {
    pub text: &'a str,
    pub callback_data: String
}

#[derive(new, Serialize, Debug)]
pub struct InlineKeyboardMarkup<'a> {
    pub inline_keyboard: Vec<Vec<InlineKeyboardButton<'a>>>
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ApiResponse {
    pub ok: bool,
    pub error_code: Option<u16>,
    pub description: Option<String>
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
pub struct CallbackQuery {
    pub id: String,
    pub from: User,
    pub data: Option<String>
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Update {
    pub update_id: i32,
    pub message: Option<Message>,
    pub callback_query: Option<CallbackQuery>
}

#[derive(Serialize, Deserialize, Debug)]
pub struct UpdateResponse {
    pub result: Vec<Update>
}
