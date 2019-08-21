# LocMess-Server
Server project for CMU 2016/2017

# API

| Endpoint                                                    | HTML Methods     |
| ----------------------------------------------------------- |:----------------:|
| https://cmu-locmess.appspot.com/api/                        | GET              |
| https://cmu-locmess.appspot.com/api/login/                  | POST             |
| https://cmu-locmess.appspot.com/api/logout/                 | POST             |
| https://cmu-locmess.appspot.com/api/registration/           | POST             |
| https://cmu-locmess.appspot.com/api/interests/              | GET              |
| https://cmu-locmess.appspot.com/api/users/                  | GET              |
| https://cmu-locmess.appspot.com/api/users/{username}/       | GET, PUT         |
| https://cmu-locmess.appspot.com/api/posts/                  | GET, POST        |
| https://cmu-locmess.appspot.com/api/posts/{post_id}         | GET, DELETE      |
| https://cmu-locmess.appspot.com/api/locations/              | GET, POST        |
| https://cmu-locmess.appspot.com/api/locations/{location_id} | GET, DELETE      |


# Pre requirements
* mysql (https://www.mysql.com/)
    * DATABASE: locmess
    * USER: root
    * PASSWORD: rootroot
* python2.7
    * https://www.python.org/download/
 * pip
    * https://pip.pypa.io/en/stable/
* virtualenv
    * https://virtualenv.pypa.io/en/stable/
    
# Installation
* git clone https://github.com/mrfrederico-ist/LocMess-Server.git
* cd LocMess-Server/
* virtualenv env
* source env/bin/activate
* cd cmu/
* pip install -r requirements.text
* python manage.py migrate
* python manage.py createsuperuser

# Run Server
* cd LocMess-Server
* source env/bin/activate
* python cmu/manage.py runserver
