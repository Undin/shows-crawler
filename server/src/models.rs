use schema::{users, sources, shows, subscriptions};

#[derive(Identifiable, Queryable, Insertable, Associations, Debug)]
#[has_many(subscriptions)]
#[table_name="users"]
pub struct User {
    pub id: i32,
    pub first_name: String,
}

#[derive(Identifiable, Queryable, Insertable, Associations, Debug)]
#[has_many(shows)]
#[table_name="sources"]
pub struct Source {
    pub id: i64,
    pub name: String,
}

#[derive(Identifiable, Queryable, Insertable, Associations, Debug)]
#[has_many(subscriptions)]
#[belongs_to(Source)]
#[table_name="shows"]
pub struct Show {
    pub id: i64,
    pub source_id: i64,
    pub raw_id: i64,
    pub title: String,
    pub local_title: Option<String>,
    pub last_season: Option<i32>,
    pub last_episode: Option<i32>,
}

#[derive(Queryable, Insertable, Associations, Debug)]
#[belongs_to(User, Show)]
#[table_name="subscriptions"]
pub struct Subscription {
    pub show_id: i64,
    pub user_id: i32,
}
