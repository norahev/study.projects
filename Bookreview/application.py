import os, requests

from flask import Flask, session, render_template, request, flash, logging, url_for, redirect, jsonify
from flask_session import Session
from sqlalchemy import create_engine
from sqlalchemy.orm import scoped_session, sessionmaker
from werkzeug.security import check_password_hash, generate_password_hash
from flask_bcrypt import Bcrypt
from helper import *

app = Flask(__name__)
bcrypt = Bcrypt()

# Check for environment variable
if not os.getenv("DATABASE_URL"):
    raise RuntimeError("DATABASE_URL is not set")

# Configure session to use filesystem
app.config["SESSION_PERMANENT"] = False
app.config["SESSION_TYPE"] = "filesystem"
Session(app)

# Set up database
engine = create_engine(os.getenv("DATABASE_URL"))
db = scoped_session(sessionmaker(bind=engine))


@app.route("/", methods = ["GET", "POST"])
@login_required
def index():
    username = ''.join(session.get('username'))
    error = "Search for the books that interest you."
    data = request.form.get("s")
    booksearch = []
    books = db.execute("SELECT * FROM books ORDER BY year LIMIT 5").fetchall()
    if request.method == "POST":
        booksearch = db.execute("SELECT * FROM books WHERE isbn ILIKE :data OR title ILIKE :data OR author ILIKE :data",
                     {"data":"%"+data+"%"}).fetchall()
        
        if not booksearch:
            error = "Nothing found.Try again."
    return render_template("index.html", booksearch=booksearch, data=data, books=books, error=error, username=username)

@app.route("/isbn/<string:isbn>", methods = ["GET", "POST"])
@login_required
def book(isbn):
    username = ''.join(session.get('username'))
    url = request.referrer
    message = "Add a review."
    user_review = db.execute("SELECT * FROM reviews WHERE isbn=:isbn AND username=:username", {"isbn":isbn, "username":username}).fetchone()
    reader_reviews = db.execute("SELECT review, rating, username FROM books JOIN reviews ON reviews.isbn = books.isbn WHERE books.isbn=:isbn", {"isbn":isbn}).fetchall()
    book = db.execute("SELECT * FROM books WHERE isbn=:isbn", {"isbn":isbn}).fetchone()
    session["reviews"]=[]
    if request.method == "POST" and not user_review:
        review = request.form.get('reviewtext')
        rating = request.form.get('stars')
        db.execute("INSERT INTO reviews (isbn, review, rating, username) VALUES (:isbn, :review, :rating, :username)",
                  {'isbn':isbn, 'review':review, 'rating':rating, 'username':username})
        db.commit()
    if request.method == "POST" and user_review:
        message = "You cannot add a second review."
    
    res = requests.get("https://www.goodreads.com/book/review_counts.json", params={"key":"ChPTpKjTmTdvUCwJmICw", "isbns":isbn})
    average = res.json()['books'][0]['average_rating']
    count = res.json()['books'][0]['work_ratings_count']
    
    reviews = db.execute("SELECT * FROM reviews WHERE isbn=:isbn", {"isbn":isbn}).fetchall()
    for review in reviews:
        session['reviews'].append(review)  

    return render_template("book.html", book=book, url=url,average=average,reader_reviews=reader_reviews,message=message, count=count, username=username)

@app.route("/api/<string:isbn>")
@login_required
def api(isbn):
    book = db.execute("SELECT * FROM books WHERE isbn=:isbn", {"isbn":isbn}).fetchone()
    if not book:
        return jsonify({"ERROR 404":"INVALID ISBN"}), 404
    res = requests.get("https://www.goodreads.com/book/review_counts.json", params={"key":"ChPTpKjTmTdvUCwJmICw", "isbns":isbn})
    average = res.json()['books'][0]['average_rating']
    count = res.json()['books'][0]['work_ratings_count']
    return jsonify ({
        "title": book.title,
        "author": book.author,
        "isbn": book.isbn,
        "year": book.year,
        "review_count": count,
        "score": average
    })
@app.route("/register", methods = ["GET", "POST"])
def register():
    if request.method == "POST":
        name = request.form.get("username")
        password = request.form.get("password")
        confirm = request.form.get("confirm")

        hashp = bcrypt.generate_password_hash(password).decode('utf-8')
        if not name or not password:
            return "Please provide a username and a password"
        if password != confirm:
            return "Your confirmation doesn't match your password"
        result = db.execute("SELECT username FROM users").fetchall()
        for i in range(len(result)):
            if result[i]["username"] == name:
                return "Username already exists"    
        db.execute("INSERT INTO users (username, hash) VALUES (:username, :hash)", {"username":name, "hash":hashp})
        db.commit()
        flash("You are registered now", "success")
    return render_template("register.html")
@app.route("/login", methods = ["GET", "POST"])
def login():
    session.clear()
    if request.method == "POST":
        name = request.form.get("user")
        password = request.form.get("hashword")
        result = db.execute("SELECT username FROM users WHERE username=:name", {"name":name}).fetchone()
        if not result:
            return "Username doesn't exist"
        pcheck = db.execute("SELECT hash FROM users WHERE username=:name", {"name":name}).fetchone()
        if result:
            formpass = ''.join(pcheck)
            if bcrypt.check_password_hash(formpass, password):
                session["username"] = result
                return redirect("/")
            else:
                return "Wrong password."
        else:
            return "Unsuccessful login, try again"
    return render_template("login.html")
@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))

        


