--
-- PostgreSQL database dump
--

-- Dumped from database version 14.12 (Homebrew)
-- Dumped by pg_dump version 14.12 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Drop existing table if it exists
--

DROP TABLE IF EXISTS public.projects CASCADE;

--
-- Create table
--

CREATE TABLE public.projects (
    id integer NOT NULL,
    project_name text NOT NULL,
    project_date text NOT NULL,
    description text,
    img_url text,
    tags text,
    PRIMARY KEY (id)
);

--
-- Create sequence
--

CREATE SEQUENCE public.projects_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.projects_id_seq OWNED BY public.projects.id;

--
-- Set default value for the id column
--

ALTER TABLE ONLY public.projects ALTER COLUMN id SET DEFAULT nextval('public.projects_id_seq'::regclass);

--
-- Insert data
--

COPY public.projects (id, project_name, project_date, description, img_url, tags) FROM stdin;
1	Personal Website	Jun 2024	A personal portfolio website showcasing my projects and skills.	/files/image1.jpg	fullstack portfolio web-development
2	Happy Pets	Apr 2024	A full-stack website to simulate an adoption center.	/files/image2.jpg	fullstack web-development
3	LibCheck	Dec 2023	A web-app that automates book searches on a library website.	/files/image3.jpg	automation web-development web-scraping
\.

--
-- Set sequence value
--

SELECT pg_catalog.setval('public.projects_id_seq', 3, true);
