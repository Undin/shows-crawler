use schema::{users, shows, subscriptions};

#[derive(new, Identifiable, Queryable, Insertable, Associations, Debug)]
#[has_many(subscriptions)]
#[table_name="users"]
pub struct User {
    pub id: i32,
    pub first_name: String,
    pub chat_id: i64,
    pub active: bool
}

#[derive(new, Identifiable, Queryable, Insertable, Associations, Debug)]
#[has_many(subscriptions)]
#[table_name="shows"]
pub struct Show {
    pub id: i64,
    pub raw_id: i64,
    pub title: String,
    pub source_name: String,
    pub local_title: Option<String>,
    pub last_season: Option<i32>,
    pub last_episode: Option<i32>,
}

#[derive(new, Queryable, Insertable, Associations, Debug)]
#[belongs_to(User, Show)]
#[table_name="subscriptions"]
pub struct Subscription {
    pub show_id: i64,
    pub user_id: i32,
}
