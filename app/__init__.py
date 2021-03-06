import os
import threading
import time

import requests
from dotenv import load_dotenv, find_dotenv
from flask import Flask
from flask_pymongo import PyMongo

from app.api.v1.admin import admin_endpoint
from app.api.v1.debug import debug_endpoint
from app.api.v1.messages import messages_endpoint
from app.api.v1.services import services_endpoint
from app.api.v1.topic import topic_endpoint, Topic
from app.api.v1.users import user_endpoint
from app.frontend.frontend import frontend

frontend_dir = os.path.abspath("templates/")
static_dir = os.path.abspath("static/")

app = Flask(__name__, template_folder=frontend_dir, static_folder=static_dir)

app.register_blueprint(frontend)
app.register_blueprint(debug_endpoint, url_prefix="/api/v1/debug")
app.register_blueprint(user_endpoint, url_prefix="/api/v1/users")
app.register_blueprint(messages_endpoint, url_prefix="/api/v1/messages")
app.register_blueprint(services_endpoint, url_prefix="/api/v1/services")
app.register_blueprint(admin_endpoint, url_prefix="/api/v1/admin")
app.register_blueprint(topic_endpoint, url_prefix="/api/v1/topic")

load_dotenv(find_dotenv())

if "MONGO_URL" in os.environ:
    app.config["MONGO_URI"] = os.environ["MONGO_URL"]
else:
    app.config["MONGO_URI"] = "mongodb://localhost:27017/loinnir"

app.secret_key = os.environ["ADMIN_SECRET"]

mongo = PyMongo(app)
