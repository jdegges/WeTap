import cgi
import os
from django.utils import simplejson as json

from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db

class Survey(db.Model):
    user = db.UserProperty()
    timestamp =     db.DateTimeProperty(auto_now_add=True)
    q_taste =       db.StringProperty()
    q_visibility =  db.StringProperty()
    q_operable =    db.StringProperty()
    q_flow =        db.StringProperty()
    q_style =       db.StringProperty()
    longitude =     db.StringProperty()
    latitude =      db.StringProperty()
    time =          db.StringProperty()
    version =       db.StringProperty()
    photo =         db.BlobProperty()

class MainPage(webapp.RequestHandler):
    def get(self):
        surveys = db.GqlQuery("SELECT * FROM Survey ORDER BY timestamp DESC LIMIT 10")
        for s in surveys:
            self.response.headers['Content-Type'] = 'image/jpeg'
            self.response.out.write(s.photo)
            return
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.out.write('No data has been uploaded :[')

class UploadSurvey(webapp.RequestHandler):
    def post(self):
        s = Survey()

        if users.get_current_user():
            s.user = users.get_current_user()

        s.q_taste = self.request.get('q_taste')
        s.q_visibility = self.request.get('q_visibility')
        s.q_operable = self.request.get('q_operable')
        s.q_flow = self.request.get('q_flow')
        s.q_style = self.request.get('q_style')
        s.longitude = self.request.get('longitude')
        s.latitude = self.request.get('latitude')
        s.time = self.request.get('time')
        s.version = self.request.get('version')
        s.photo = db.Blob(self.request.get('file'))
        s.put()
        self.redirect('/')

class GetPointSummary(webapp.RequestHandler):
    def get(self):
        surveys = db.GqlQuery("SELECT * FROM Survey ORDER BY timestamp DESC LIMIT 10")
        d = {}
        i = 0
        for s in surveys:
            e = {}
            e['latitude'] = s.latitude
            e['longitude'] = s.longitude
            e['q_taste'] = s.q_taste
            e['key'] = str(s.key())
            e['version'] = s.version

            d[i] = e;
            i = i + 1

        self.response.headers['Content-type'] = 'text/plain'
        if i > 0:
            self.response.out.write(json.dumps(d))
        else:
            self.response.out.write("no data so far")

class GetAPoint(webapp.RequestHandler):
    def get(self):
        self.response.headers['Content-type'] = 'text/plain'
        req_key = self.request.get('key')
        if req_key != '':
            try :
                db_key = db.Key(req_key)
                surveys = db.GqlQuery("SELECT * FROM Survey WHERE __key__ = :1", db_key)
                for s in surveys:
                    e = {}
                    e['photo'] = "http://we-tap.appspot.com/get_image_thumb?key=" + req_key;
                    e['q_flow'] = s.q_flow
                    e['q_operable'] = s.q_operable
                    e['q_style'] = s.q_style
                    e['q_taste'] = s.q_taste
                    e['q_visibility'] = s.q_visibility
                    e['version'] = s.version
                    self.response.out.write(json.dumps(e))
                    return
            except (db.Error):
                self.response.out.write("No data has been uploaded :[")
                return
        self.response.out.write("No data has been uploaded :[")

class GetAnImage(webapp.RequestHandler):
    def get(self):
        self.response.headers['Content-type'] = 'image/jpeg'
        req_key = self.request.get('key')
        if req_key != '':
            try :
                db_key = db.Key(req_key)
                surveys = db.GqlQuery("SELECT * FROM Survey WHERE __key__ = :1", db_key)
                for s in surveys:
                    self.response.out.write(s.photo)
                    return
            except (db.Error):
                return
        return

class GetImageThumb(webapp.RequestHandler):
    def get(self):
        self.response.headers['Content-type'] = 'text/html'
        req_key = self.request.get('key')
        self.response.out.write("<html><body><img src=\"http://we-tap.appspot.com/get_an_image?key=")
        self.response.out.write(req_key)
        self.response.out.write("\" width=\"180\" height=\"130\"></body></html>")

application = webapp.WSGIApplication(
                                     [('/', MainPage),
                                      ('/upload_survey', UploadSurvey),
                                      ('/get_point_summary', GetPointSummary),
                                      ('/get_a_point', GetAPoint),
                                      ('/get_an_image', GetAnImage),
                                      ('/get_image_thumb', GetImageThumb)],
                                     debug=True)

def main():
    run_wsgi_app(application)

if __name__ == "__main__":
    main()
