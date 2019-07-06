# postgrest-ui

postgrest-ui is a library of Reagent components for interfacing with databases
using [PostGREST](http://postgrest.org).

The focus of this library is to create reusable and customizable components for common
UI tasks, like listing views and forms, and provide a wrapper for the HTTP interface
via Fetch.

The goal is to keep the components general purpose and allow them to be integrated into any
frontend framework. The components can keep their own state, or use customer provided state
(with `:state` and `:set-state!` keys in options).

## Running the example

0. Set up PostgreSQL
1. Create [dvdrental sample database](http://www.postgresqltutorial.com/postgresql-sample-database/)
2. Run [PostgREST](http://postgrest.org)
3. Start figwheel: `clj -m figwheel.main -b dev -r`
