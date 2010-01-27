import cgi
import os
from django.utils import simplejson as json

from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from google.appengine.ext.webapp import template

def decode_surveys(surveys):
    decoded = []
    for s in surveys:
        item = {}
        item['q_taste'] = decode_survey("taste", s.q_taste)
        item['q_visibility'] = decode_survey("visibility", s.q_visibility)
        item['q_operable'] = decode_survey("operable", s.q_operable)
        item['q_flow'] = decode_survey("flow", s.q_flow)
        item['q_style'] = decode_survey("style", s.q_style)
        item['q_location'] = decode_survey("location", s.q_location)
        item['longitude'] = s.longitude
        item['latitude'] = s.latitude
        item['key'] = str(s.key())
        decoded.append(item)
    return decoded

def decode_survey(q, v):
    try:
        ret = {
            'taste':
                lambda v: {
                    '0': lambda: "Same as home tap",
                    '1': lambda: "Better",
                    '2': lambda: "Worse",
                    '3': lambda: "Can't answer"
                }[v](),
            'visibility':
                lambda v: {
                    '0': lambda: "Visible",
                    '1': lambda: "Hidden"
                }[v](),
            'operable':
                lambda v: {
                    '0': lambda: "Working",
                    '1': lambda: "Broken",
                    '2': lambda: "Needs repair"
                }[v](),
            'flow':
                lambda v: {
                    '0': lambda: "Strong",
                    '1': lambda: "Trickle",
                    '2': lambda: "Too strong"
                }[v](),
            'style':
                lambda v: {
                    '0': lambda: "Refilling",
                    '1': lambda: "Drinking",
                    '2': lambda: "Both"
                }[v](),
            'location':
                lambda v: {
                    '0': lambda: "Indoor",
                    '1': lambda: "Outdoors"
                }[v]()
        }[q](v)
        return ret
    except KeyError:
        return "Not rated"

class Survey(db.Model):
    user = db.UserProperty()
    timestamp =     db.DateTimeProperty(auto_now_add=True)
    q_taste =       db.StringProperty()
    q_visibility =  db.StringProperty()
    q_operable =    db.StringProperty()
    q_flow =        db.StringProperty()
    q_style =       db.StringProperty()
    q_location =    db.StringProperty()
    longitude =     db.StringProperty()
    latitude =      db.StringProperty()
    time =          db.StringProperty()
    version =       db.StringProperty()
    photo =         db.BlobProperty()

class HomePage(webapp.RequestHandler):
    def get(self):
        path = os.path.join (os.path.dirname(__file__), 'views/home.html')
        self.response.out.write (template.render(path, {}))

class MapPage(webapp.RequestHandler):
    def get(self):
        surveys = Survey.all().fetch(100)
        decoded = decode_surveys (surveys)
        template_values = { 'surveys' : decoded }
        path = os.path.join (os.path.dirname(__file__), 'views/map.html')
        self.response.out.write (template.render(path, template_values))

class ClientsPage(webapp.RequestHandler):
    def get(self):
        path = os.path.join (os.path.dirname(__file__), 'views/clients.html')
        self.response.out.write (template.render(path, {}))

class AboutPage(webapp.RequestHandler):
    def get(self):
        path = os.path.join (os.path.dirname(__file__), 'views/about.html')
        self.response.out.write (template.render(path, {}))

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
        s.q_location = self.request.get('q_location')
        s.longitude = self.request.get('longitude')
        s.latitude = self.request.get('latitude')
        s.time = self.request.get('time')
        s.version = self.request.get('version')

        file_content = self.request.get('file')
        try:
            s.photo = db.Blob(file_content)
        except TypeError:
            s.photo = ''

        s.put()
        self.redirect('/')

class GetPointSummary(webapp.RequestHandler):
    def get(self):
        surveys = db.GqlQuery("SELECT * FROM Survey ORDER BY timestamp DESC LIMIT 100")
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
                    e['q_location'] = s.q_location
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
                                     [('/', HomePage),
                                      ('/map', MapPage),
                                      ('/clients', ClientsPage),
                                      ('/about', AboutPage),
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
