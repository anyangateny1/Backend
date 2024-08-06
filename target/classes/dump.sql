PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE projects(id INTEGER PRIMARY KEY, project_name TEXT NOT NULL, project_date TEXT NOT NULL, desc TEXT, img_url TEXT, tags TEXT);
INSERT INTO projects VALUES(1,'Personal Website','Jun 2024','A personal portfolio website showcasing my projects and skills.','/files/image1.jpg','fullstack portfolio web-development');
INSERT INTO projects VALUES(2,'Happy Pets','Apr 2024','A full-stack website to simulate an adoption center.','/files/image2.jpg','fullstack web-development');
INSERT INTO projects VALUES(3,'LibCheck','Dec 2023','A web-app that automates book searches on a library website.','/files/image3.jpg',replace('automation web-development web-scraping\n','\n',char(10)));
COMMIT;
