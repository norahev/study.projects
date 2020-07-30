# Project 1

Web Programming with Python and JavaScript

My Database: 
I have 3 tables in my database:users(id, username, hash), books(isbn, title, author, year) and reviews(isbn, review, rating, username). 
import.py:
This is where I import the book.csv file into my Heroku database. 
helper.py:
This is a function to make sure someone is logged in. 
application.py:
This is where I have my code for registering, loggin in, searching, creating API,  leaving reviews. 

Users can register to my page and then they can log in using their username and password. I only store a hash of their password not the actual password they provide. After logging in they are taken to their homepage where they see a list of 5 books automatically displayed to them. At the top of the page there is a search function where users can look up books by writing in the title, author, isbn or only part of these informations. The user then gets back the search result, if there's any, else they get a message that nothing has been found. The title's of the books are clickable and they take the user to the page of the book, where it is possible to leave a review and rating, but only once for one book. Also this is where users can request an API for the given book, by clicking the API button. Besides leaving a review this is where other reviews can be found, if there is any. 

https://www.youtube.com/watch?v=EOu5_IzsRpU video of the webapp.
