create table users (id serial primary key, name text, email text);
insert into users (name,email) values ('Mary Smith', 'mary@domain.com');
insert into users (name,email) values ('John Doe', 'john@domain.com');

create table categories (id integer primary key, description text);
insert into categories (id,description) values (1, 'computer-science');
insert into categories (id,description) values (2, 'math');
insert into categories (id,description) values (3, 'biology');


