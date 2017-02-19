extern crate serde_json;

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
