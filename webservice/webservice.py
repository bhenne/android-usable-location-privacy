#!/usr/bin/env python

"""
 Usable Location Privacy Extension
  Copyright (C) 2014 B. Henne, C. Kater,
    Distributed Computing & Security Group,
    Leibniz Universitaet Hannover, Germany
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""

import cherrypy
import json
import sqlite3
import os
import httplib2
import oauth2client.client
from oauth2client.client import AccessTokenCredentials
from apiclient.discovery import build
import hashlib
import sys
import math

__author__ = "B. Henne, C. Kater"
__copyright__ = "(c) 2014, DCSec, Leibniz Universitaet Hannover, Germany"
__license__ = "Apache 2.0"

ssl_path = '/etc/ssl'
my_path = os.path.dirname(os.path.realpath(__file__))

server_config = {
    'server.socket_host': '0.0.0.0',
    'server.socket_port': 8443,
    'server.ssl_module': 'pyopenssl',
    'server.ssl_certificate': os.path.join(ssl_path, 'certs', 'ulpa-service.crt'),
    'server.ssl_certificate_chain': os.path.join(ssl_path, 'certs', 'sub.class1.server.ca.pem'),
    'server.ssl_private_key': os.path.join(ssl_path, 'private', 'ulpa-service.key'),
    'tools.encode.encoding': 'UTF-8',
    'log.access_file': os.path.join(my_path, 'access.log'),
    'log.error_file': os.path.join(my_path, 'error.log'),
    'log.screen': False,
}

class Webservice:
    db_path = 'ulpa_configurations.db'

    def __init__(self):
        db = sqlite3.connect(self.db_path)
        cursor = db.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS configuration(app text, user text, preset integer, radius integer, PRIMARY KEY(app, user))''')
        cursor.close()
        db.close()

    def index(self):
        return "Usable Location Privacy Framework Webservice"
    index.exposed = True

    def filter(self, d):
	for k in ('user', 'username', 'password'):
            if k in d:
                del d[k]
        return d

    @cherrypy.expose()
    def get(self, **kwargs):
        """

        :param kwargs:
        :return:
        """
        kwargs = self.filter(kwargs)
        db = sqlite3.connect(self.db_path)
        cursor = db.cursor()

        #get information from calling android device
        input = json.loads(json.dumps(kwargs))
        try:
            app = input['app']
        except KeyError:
            cursor.close()
            db.close()
            return "error: invalid input"
        online = False
        try:
            street = input['street']
            district = input['district']
            city = input['city']
        except KeyError:
            online = True

        statistic = {}
        statistic['0'] = 0;
        statistic['1'] = 0;
        statistic['2'] = 0;
        statistic['3'] = 0;
        statistic['4'] = 0;
        if online:
            cursor.execute('SELECT preset, count(preset) FROM configuration WHERE app = ? AND radius = -2 GROUP BY preset', (app,))
            for row in cursor:
                statistic[str(row[0])] = row[1]
        else :
            cursor.execute('SELECT preset, radius FROM configuration WHERE app = ? AND radius > -2', (app,))
            for row in cursor:
                preset = row[0]
                radius = int(row[1])
                print radius, " ", preset

                if preset == 0 or preset == 4:
                    statistic[str(preset)] = statistic[str(preset)] + 1
                else:
                    minDev = sys.maxint;
                    bestpreset = -1
                    streetDev = math.fabs(radius - int(street))
                    if streetDev < minDev:
                        minDev = streetDev
                        bestpreset = 1
                    districtDev = math.fabs(radius - int(district))
                    if districtDev < minDev:
                        minDev = districtDev
                        bestpreset = 2
                    cityDev = math.fabs(radius - int(city))
                    if cityDev < minDev:
                        bestpreset = 3
                    statistic[str(bestpreset)] = statistic[str(bestpreset)] + 1
        cursor.close()
        db.close()
        return json.dumps(statistic)

    @cherrypy.expose()
    def set(self, **kwargs):
        kwargs = self.filter(kwargs)
        db = sqlite3.connect(self.db_path)
        cursor = db.cursor()

        #get information from calling android device
        input = json.loads(json.dumps(kwargs))
        try:
            app = input['app']
            preset = input['preset']
            accesstoken = input['accesstoken']
        except KeyError:
            cursor.close()
            db.close()
            return "error: invalid input"
        try:
            radius = input['radius']
        except KeyError:
            radius = -2

        #get user id from google with access token
        try:
            credentials = AccessTokenCredentials(accesstoken, 'my-user-agent/1.0')
            http = httplib2.Http()
            http = credentials.authorize(http)
            service = build('oauth2', 'v2', http=http)
            userinfo = service.userinfo().get().execute()
            user = hashlib.sha256(userinfo['id']).hexdigest()
        except oauth2client.client.AccessTokenCredentialsError:
            cursor.close()
            db.close()
            return "error: The access_token is expired or invalid and can't be refreshed"

        try:
            cursor.execute("INSERT INTO configuration(app, user, preset, radius) VALUES(?, ?, ?, ?)", (app, user, preset, radius,))
            answer = "insert successfull"
        except sqlite3.IntegrityError:
            cursor.execute("UPDATE configuration SET preset = ?, radius = ? WHERE app = ? AND user = ?", (preset, radius, app, user, ))
            answer = "update successfull"

        db.commit()
        cursor.close()
        db.close()
        return answer

    @cherrypy.expose()
    def info(self, **kwargs):
        info = {}
        info['shareSettings'] = True
        info['showCommunityAdvice'] = True
        return json.dumps(info)

if __name__ == '__main__':
    cherrypy.config.update(server_config)
    cherrypy.tree.mount(Webservice(), '/', config=None)
    if hasattr(cherrypy.engine, 'block'):
        # 3.1 syntax
        cherrypy.engine.start()
        cherrypy.engine.block()
    else:
        # 3.0 syntax
        cherrypy.server.quickstart()
        cherrypy.engine.start()
