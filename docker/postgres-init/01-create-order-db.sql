-- Her servis kendi veritabanına sahip. mydatabase POSTGRES_DB ile oluşur; order_db burada.
-- Not: init scriptleri yalnızca Postgres volume'ü boşken (ilk kurulumda) çalışır.
CREATE DATABASE order_db OWNER myuser;
