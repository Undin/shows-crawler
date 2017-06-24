use std::fmt::{Display, Formatter, Result};
use itertools::Itertools;

#[derive(Debug, PartialEq, Eq, PartialOrd, Ord)]
pub enum PermissionLevel {
    User,
    Superuser
}

#[derive(Debug)]
pub struct Command {
    pub name: &'static str,
    pub args: &'static [&'static str],
    pub message: &'static str,
    pub permission_level: PermissionLevel
}

#[macro_export]
macro_rules! command {
    ($name: expr, $args: expr, $message: expr, $level: expr) => {
        Command { name: $name, args: $args, message: $message, permission_level: $level }
    }
}

impl Display for Command {
    fn fmt(&self, f: &mut Formatter) -> Result {
        let name = self.name.replace("_", "\\_");
        if self.args.len() == 0 {
            write!(f, "/{} - {}", name, self.message)
        } else {
            let args = self.args.iter()
                .map(|arg| format!("<{}>", arg))
                .join(" ");
            write!(f, "/{} {} - {}", name, args, self.message)
        }
    }
}
