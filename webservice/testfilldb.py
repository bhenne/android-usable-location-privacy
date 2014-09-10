#!/usr/bin/env python
#
# Fill webservice db with test data

import sqlite3
import hashlib
import random

__author__ = "B. Henne"
__copyright__ = "(c) 2014, DCSec, Leibniz Universitaet Hannover, Germany"
__license__ = "Apache 2.0"

apps = { 'android': (0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1),
         'com.cyanogenmod.lockclock': (1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4),
         'com.google.android.apps.genie.geniewidget': (0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3),
}

class testfill(object):

    db_path = 'ulpa_configurations.db'

    def __init__(self):
        db = sqlite3.connect(self.db_path)
        cursor = db.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS configuration(app text, user text, preset integer, radius integer default -2, PRIMARY KEY(app, user))''')
        cursor.close()
        db.close()

    def fill(self, **kwargs):
        db = sqlite3.connect(self.db_path)
        cursor = db.cursor()

        for app, presets in apps.items():
            for preset in presets:
                user = hashlib.sha256(str(random.random())).hexdigest()
		offline = not random.getrandbits(1)
		if offline == True:
                    radius = "-2"
		else:
		    if preset == 1:
			radius = random.randint(100, 500)
                    elif preset == 2:
                        radius = random.randint(800, 5000)
                    elif preset == 3:
                        radius = random.randint(4000,20000)
                    else:
                     radius = -2
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

if __name__  == '__main__':
    testfill().fill()
