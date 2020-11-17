import datetime
from . import db


class User(db.Model):
    __tablename__ = 'users'
    # Unique identifier (different from username, should never change)
    id = db.Column(db.Integer, primary_key=True)
    # Username, note the `unique` constraint
    username = db.Column(db.String, unique=True)
    # Name
    name = db.Column(db.String)
    # Password
    password = db.Column(db.String)

    # TODO: Add other fields we may need

    def __repr__(self):
        return "<User('%s', '%s')>" % (self.username, self.name)


class SleepRecord(db.Model):
    __tablename__ = 'sleep_records'

    id = db.Column(db.Integer, primary_key=True)

    user_id = db.Column(db.Integer, db.ForeignKey('users.id'))
    user = db.relationship('User', backref='sleep_records')

    raw_red = db.Column(db.Float) #Important (server and app-side): raw data received as red and IR
    raw_ir = db.Column(db.Float)

    bpm = db.Column(db.Float)
    hrv = db.Column(db.Float)
    spo2 = db.Column(db.Float)
    rr = db.Column(db.Float)
    sleeptime = db.Column(db.Float)


    created_at = db.Column(db.DateTime, default=datetime.datetime.utcnow)


class SleepSession(db.Model):
    __tablename__ = 'sleep_sessions'
    id = db.Column(db.Integer, primary_key=True)

    user_id = db.Column(db.Integer, db.ForeignKey('users.id'))
    user = db.relationship('User', backref='sleep_sessions')

    start_time = db.Column(db.DateTime, default=None)
    end_time = db.Column(db.DateTime, default=None)

    created_at = db.Column(db.DateTime, default=datetime.datetime.utcnow)

    def length_seconds(self):
        end = datetime.datetime.utcnow() if self.end_time is None else self.end_time
        start = self.start_time
        return (end - start).total_seconds()
