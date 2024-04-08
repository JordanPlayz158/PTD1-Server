import os

try:
    import dotenv

    dotenv.load_dotenv(os.getcwd() + '/src/local.env')
except ImportError:
    print("Failed to import dotenv ; will not load `local.env` even if it is present")

from flask import Flask, render_template, request
from src.request_handler import handle_request

app = Flask(__name__, '/')
print("Running PTD3 Server")


@app.route('/')
@app.route('/index.html')
def index():
    return render_template('index.html')


# Manage (PTD3 Story) Save
# For original SWF
@app.route('/php/ptd3_save_1.php', methods=['GET', 'POST'])
# For etrotta SWF
@app.route('/ptd3save/<username>', methods=['GET', 'POST'])
def ptd_save(username=None):
    # Save/Load/Delete/List saves, meant to speak with the game client
    return handle_request(request.form).encode()


# TODO IMPLEMENT MYSTERY GIFT?
# For original SWF
@app.route('/ptd3/gameFiles/get_mystery_gift.php')
# For etrotta SWF
@app.route('/mystery_gift')
def mystery_gift():
    return 'mgn=0&mgs=0'
