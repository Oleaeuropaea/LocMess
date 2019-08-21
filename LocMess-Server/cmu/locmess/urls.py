from django.conf.urls import url, include

from rest_framework.routers import SimpleRouter
from rest_auth.views import LoginView, LogoutView

from . import views, views_api

router = SimpleRouter()
router.register(r'users', views_api.UserViewSet)
router.register(r'posts', views_api.PostViewSet)
router.register(r'interests', views_api.InterestViewSet)
router.register(r'locations', views_api.LocationViewSet)


urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^api/$', views_api.api_root, name='api-root'),
    url(r'^api/', include(router.urls)),
    url(r'^api/login/$', LoginView.as_view(), name='rest_login'),
    url(r'^api/logout/$', LogoutView.as_view(), name='rest_logout'),
    url(r'^api/registration/', include('rest_auth.registration.urls'))
]

urlpatterns += [
    url(r'^api/users/(?P<username>[^/.]+)/interests/$', views_api.UserInterests.as_view(), name='user-interests-list'),
    url(r'^api/users/(?P<username>[^/.]+)/posts/$', views_api.UserPosts.as_view(), name='user-posts-list'),
]
