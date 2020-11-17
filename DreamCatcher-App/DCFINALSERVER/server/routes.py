from flask import request, session
from . import app, db
from .models import User, SleepRecord, SleepSession
import datetime
import tensorflow.compat.v1 as tf
import tensorflow.keras.layers as layers
import numpy as np
from scipy.signal import find_peaks
import json
#from datetime import datetime  <- why did you import datetime from datetime.datetime (?) It worked well before though.. (maybe while merging, had these overlapping imports
from server.runningstat import RunningStats
from collections import deque
from scipy.interpolate import interp1d
print("[INFO] Loading trained weights...") #TODO Solve music on/off problem (inf server)


# region inference_server


WINDOW_SIZE = 512
KERNEL_SIZE = 16
FILTERS = 64

DIALATE = 10
PLATEAU = None
HT = 0.75
DIST = 20

DAYTIME_FLAG = False
sleepingtime = dict()
last_req = dict()
last_ss = dict()
hrv = dict()
hrv_daytime = dict()


def getdc(ppg):
    peaks, _ = find_peaks(-ppg, distance=25, width=10)
    np.diff(peaks)
    x = np.linspace(0, len(ppg), num=len(peaks), endpoint=True)
    xx = np.linspace(0, len(ppg), num=len(ppg))
    f = interp1d(x, ppg[peaks], kind="cubic")
    return f(xx)


def spo2(red, ir):
    rdc = getdc(red)
    irdc = getdc(ir)
    z = np.mean((red / rdc) / (ir / irdc))
    spo2 = (-45.06*z + 30.354) * z + 94.845 + 18.3
    if spo2 > 99: spo2 = 99  # ...
    return spo2 # FIXME: FIXME...


class Inferencer:
    def __init__(self, weights_path):
        tf.disable_v2_behavior()
        self.x = tf.placeholder(tf.float32, [None, WINDOW_SIZE, 1])
        with tf.variable_scope("g"):
            g = layers.Conv1D(FILTERS, KERNEL_SIZE, padding="same", activation=tf.nn.leaky_relu)(self.x)
            g = layers.Conv1D(FILTERS, KERNEL_SIZE, padding="same", activation=tf.nn.leaky_relu)(g)
            g = layers.BatchNormalization()(g)
            g = layers.Conv1D(FILTERS, KERNEL_SIZE, padding="same", activation=tf.nn.leaky_relu)(g)
            g = layers.BatchNormalization()(g)
            g = layers.Conv1D(FILTERS, KERNEL_SIZE, padding="same", activation=tf.nn.leaky_relu)(g)
            g = layers.BatchNormalization()(g)
            g = layers.Conv1D(1, KERNEL_SIZE, padding="same", activation=tf.nn.leaky_relu)(g)
            g = layers.Dense(1)(g)

        self.g = g
        self.sess = tf.Session()
        var_g = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope="g")
        self.saver = tf.train.Saver(var_g)
        self.saver.restore(self.sess, weights_path)

    @staticmethod
    def _build_batch(wave, n_batch, pad):
        batch = np.empty([n_batch, WINDOW_SIZE])
        cursor = 0
        for i in range(n_batch):
            w = wave[cursor:cursor + WINDOW_SIZE]
            m = np.min(w)
            M = np.max(w)
            w = (w - m) / (M - m)
            batch[i] = w
            cursor += WINDOW_SIZE - 2 * pad
        batch = np.expand_dims(batch, 2)
        return batch

    @staticmethod
    def get_batch_len(n_batch, pad=50):
        return n_batch * (WINDOW_SIZE - pad)

    def infer(self, wave, n_batch, pad=50):
        print("1")
        batch = self._build_batch(wave, n_batch, pad)
        print("2")
        inf = self.sess.run(self.g, {self.x: batch})
        print("3")
        inf = np.squeeze(inf, 2)
        print("4")
        processed = np.empty([n_batch, WINDOW_SIZE - 2 * pad])
        print("N BATCH", n_batch)
        for i in range(n_batch): # <-- ???? It's definitely iterable!
            print("IIIIIIIII" * 20)
            processed[i] = inf[i][pad:-pad]
        print("DONE"*5)
        peaks, _ = find_peaks(processed.flatten(), distance=DIST, height=HT)
        print("PEAKS*10")
        diff = np.diff(peaks)

        if len(diff) == 0:
            return [], -1, -1

        rr = diff
        rr *= DIALATE
        rr_interval = np.mean(rr)
        hr = 1 / (rr_interval / 6e+4)
        return rr, hr, rr_interval


inf = Inferencer("weights/g-39000")
print('[INFO] Done.')


def req(name, is_sleeping, force=False):
    global sleepingtime, last_req
    cur_time = datetime.datetime.utcnow()
    if (not name in last_req.keys()) or force:
        print(name, 1234567890)
        last_req[name] = cur_time
        last_ss[name] = False  # Users can't turn on their device when they are sleeping (Hmm)
        sleepingtime[name] = 0.0
        hrv[name] = deque(maxlen=100)
        hrv_daytime[name] = RunningStats()
    else:
        interval = (cur_time - last_req[name]).total_seconds()
        if is_sleeping:
            if last_ss[name]:
                sleepingtime[name] += interval
            else:
                last_ss[name] = True
        else:
            last_ss[name] = False
        last_req[name] = cur_time


def process_response(username, rr, hr, rr_interval, red, ir):
    global hrv
    if len(rr) == 0:
        ret = dict()
        ret["spo2"] = -1
        ret["hr"] = -1
        ret["hrv"] = -1
        ret["rr"] = -1
        ret["status"] = 1  #
        ret["sleepingtime"] = -1 # My mistake
        ret["sleeping"] = -1  # paste tese 'ret'spytho
        return ret  # invalid data <<<<<<<<< This part

    if username not in last_req.keys():
        req(username, None)  # Kind of odd hacking, but it **definitely** works

    for i in rr:
        hrv[username].append(i)
    print("HRV"*10 + "++++", hrv[username])
    hrv_5min = np.std(hrv[username])# <-- suspicious


    is_sleeping = False

    if DAYTIME_FLAG:
        for i in rr:
            hrv_daytime[username].push(i)
    else:
        if hrv_5min + 50 < hrv_daytime[username].standard_deviation():
            is_sleeping = True
        req(username, is_sleeping)
        # req function is used ONLY for calculating sleeping time.
        # So, we don't have to call req in the daytime!
    ret = dict()
    ret["spo2"] = spo2(red, ir)
    ret["hr"] = hr
    ret["hrv"] = np.std(hrv[username])
    ret["rr"] = rr_interval
    ret["status"] = 1
    ret["sleepingtime"] = sleepingtime[username]
    if is_sleeping:
        ret["sleeping"] = 1  # paste tese 'ret's
    else:
        ret["sleeping"] = -1
    return ret


@app.route("/d")
def to_daytime():
    global DAYTIME_FLAG
    DAYTIME_FLAG = True
    return app.response_class("OK", status=1234567890)


@app.route("/n")
def to_nighttime():
    global DAYTIME_FLAG
    DAYTIME_FLAG = False
    return app.response_class("OK", status=1234567890)


@app.route("/clear_db")
def force_clear():
    for i in sleepingtime.keys():
        sleepingtime[i] = 0
    return app.response_class("OK", status=1234567890)
#endregion


#region authenticate
@app.route('/', methods=['GET'])
def index():
    return 'Hello world!'


@app.route('/auth/login', methods=['POST'])
def login():
    """
    Section handling the login - JSON package received with username and password
    """
    params = request.get_json()
    if params is None:
        return {}, 400
    user = User.query.filter_by(username=params['username'], password=params['password']).first()
    if user is None:
        return {}, 401
    session['id'] = user.id
    req(user.username, False, True)
    return {
        "username": user.username,
        "name": user.name
    }


@app.route('/auth/join', methods=['POST'])
def join():
    params = request.get_json()
    if params is None:
        return {"success": False}, 400
    try:
        if User.query.filter_by(username=params['username']).count() > 0:
            return {
                       "success": False,
                       "reason": "User exists"
                   }, 400
        new_user = User(
            username=params['username'],
            password=params['password'],
            name=params['name']
        )
        db.session.add(new_user)
        db.session.commit()
    except Exception as err:
        print(err)
        return {"success": False}, 400
    return {"success": True}


@app.route('/auth/me', methods=['GET'])
def me():
    if 'id' in session:
        me = User.query.filter_by(id=session['id']).first()
        if me is None:
            return {}, 401
        return {
            "username": me.username,
            "name": me.name
        }
    return {}, 401
#endregion


@app.route('/records/main', methods=['GET'])  # FIXME: Just for highlight -------------------------------------------------------------
def records_main():
    if 'id' not in session:
        print("ID is not in session")
        return {}, 401
    me = User.query.filter_by(id=session['id']).first()
    if me is None:
        print("Me is None")
        return {}, 401

    payload = {
        "name": me.name
    }

    recent_record = SleepRecord.query.filter_by(user_id=session['id']).order_by(SleepRecord.created_at.desc()).first() #asc or desc
    recent_session = SleepSession.query.filter_by(user_id=session['id']).order_by(
        SleepSession.created_at.desc()).first()
    if recent_record is None:
        # Set value as -1 to indicate an invalid value
        payload["bpm"] = -1
        payload["hrv"] = -1
        payload["spo2"] = -1
        payload["rr"] = -1
        payload["record_time"] = ""
    else:
        payload["bpm"] = recent_record.bpm
        payload["hrv"] = recent_record.hrv
        payload["spo2"] = recent_record.spo2
        payload["rr"] = recent_record.rr
        payload["record_time"] = recent_record.created_at #when updated

    if recent_session is None:
        payload["sleep_time"] = -1
    else:
        payload["sleep_time"] = sleepingtime[me.username]#recent_session.length_seconds()

    return payload


@app.route('/records', methods=['POST'])
def records_write():
    if 'id' not in session:
        return {}, 401
    me = User.query.filter_by(id=session['id']).first()
    if me is None:
        return {}, 401
    value = request.get_json()
    #red, ir = data['red'], data['ir'] ## <<=  this is not correct, should be just like mock server's

    data = value["value"]
    pleth = data.split("\n")[:-1]  # <<--- !
    data = np.zeros([len(pleth), 2])
    data -= 1
    for i, p in enumerate(pleth):
        datum = pleth[i].split(",")
        data[i] = int(datum[0]), int(datum[1])
    data = data.T
    red, ir = data[0], data[1]  # <<- to this... OK

    new_record = SleepRecord() # working??? no.
    new_record.user_id = session['id']

    username = me.username
    print(red)
    rr, hr, intv = inf.infer(red, 1)  # batch size
    ret_dict = process_response(username, rr, hr, intv, red, ir)
    print(ret_dict)
    print(ret_dict['hr'])
    new_record.bpm = ret_dict['hr']  # ERROR: String indicies must be integers?
    new_record.hrv = np.std(hrv[username]) #standard deviation calculation. resolved.
    new_record.spo2 = ret_dict['spo2']
    new_record.rr = ret_dict['rr']  # and you need to modify these payloads
    new_record.sleeptime = sleepingtime[username]

    db.session.add(new_record)
    db.session.commit()

    return json.dumps(ret_dict), 200


@app.route('/records/list/<int:count>', methods=['GET'])
def records_list(count):
    if 'id' not in session:
        return {}, 401
    me = User.query.filter_by(id=session['id']).first()
    if me is None:
        return {}, 401
    records = SleepRecord.query.filter_by(user_id=session['id']) \
        .order_by(SleepRecord.created_at.asc()) \
        .limit(count) \
        .all()
    return {
        'result': [
            {'bpm': item.bpm, 'hrv': item.hrv, 'spo2': item.spo2, 'rr': item.rr, 'time': item.created_at}
            for item in records
        ]
    }


# region session-related-code
@app.route('/session/start', methods=['PUT'])
def start_session():
    if 'id' not in session:
        return {}, 401
    me = User.query.filter_by(id=session['id']).first()
    if me is None:
        return {}, 401
    recent_session = SleepSession.query.filter_by(user_id=session['id']) \
        .order_by(SleepSession.created_at.asc()) \
        .first()
    if recent_session is not None and recent_session.end_time is None:
        recent_session.start_time = datetime.datetime.utcnow()
        db.session.commit()
    else:
        new_session = SleepSession()
        new_session.user_id = session['id']
        new_session.start_time = datetime.datetime.utcnow()
        new_session.end_time = None
        db.session.add(new_session)
        db.session.commit()
    return {}


@app.route('/session/end', methods=['PUT'])
def end_session():
    if 'id' not in session:
        return {}, 401
    me = User.query.filter_pythby(id=session['id']).first()
    if me is None:
        return {}, 401
    recent_session = SleepSession.query.filter_by(user_id=session['id']) \
        .filter_by(end_time=None) \
        .order_by(SleepSession.created_at.asc()) \
        .first()
    if recent_session is not None:
        recent_session.end_time = datetime.datetime.utcnow()
        db.session.commit()
    return {}
#endregion

# Note: asc and desc