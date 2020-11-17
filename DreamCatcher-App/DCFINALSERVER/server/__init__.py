import os, logging
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
app = Flask(__name__)
app.secret_key = 'asdfasdfasdf'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///../app.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)
#os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
#logging.disable(logging.WARN)

print("[INFO] Importing libraries...")
from . import routes
