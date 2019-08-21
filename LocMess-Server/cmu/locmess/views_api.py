from collections import OrderedDict

from django.utils import timezone
from django.contrib.auth.models import User

from rest_framework import permissions, status
from rest_framework import viewsets
from rest_framework import mixins
from rest_framework import generics
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework.reverse import reverse
from rest_framework import exceptions

from . import serializers
from .permissions import IsAdminOrIsSelf
from . import validators
from .models import Post, Interest, Location
from .exeptions import FailedDependency


@api_view(['GET'])
def api_root(request):
    ret = OrderedDict()

    ret['login'] = reverse('rest_login', request=request)
    ret['logout'] = reverse('rest_logout', request=request)
    ret['registration'] = reverse('rest_register', request=request)
    ret['interests'] = reverse('interest-list', request=request)
    ret['users'] = reverse('user-list', request=request)
    ret['posts'] = reverse('post-list', request=request)
    ret['locations'] = reverse('location-list', request=request)

    return Response(ret)


class ListAddTimestampMixin(mixins.ListModelMixin):
    def list(self, request, *args, **kwargs):
        queryset = self.get_queryset()
        serializer = self.get_serializer(queryset, many=True)

        result = [{'result': [item for item in serializer.data]}]
        if len(result[0]['result']) == 0:
            return Response(status=status.HTTP_204_NO_CONTENT)

        # Add last update timestamp
        timestamp = float(self.queryset.latest('creation_date').creation_date.strftime('%s.%f'))
        result.insert(0, OrderedDict([('timestamp', timestamp)]))

        return Response(result, status=status.HTTP_200_OK)


class InterestViewSet(ListAddTimestampMixin,
                      mixins.RetrieveModelMixin,
                      mixins.DestroyModelMixin,
                      viewsets.GenericViewSet):
    queryset = Interest.objects.all()
    serializer_class = serializers.InterestNameSerializer
    permission_classes = (permissions.IsAuthenticated, IsAdminOrIsSelf)

    def get_queryset(self):
        if self.action == 'list':
            query_params = self.request.query_params
            if query_params:
                timestamp = validators.timestamp_query_param_validator(self.request.query_params)
                return self.queryset.filter(creation_date__gt=timestamp).order_by().values('name').distinct()
            return self.queryset.order_by().values('name').distinct()
        return self.queryset.all()

    def get_serializer_class(self):
        if self.action != 'list':
            return serializers.InterestFullSerializer
        return self.serializer_class

    def destroy(self, request, *args, **kwargs):
        instance = self.get_object()
        request.user.interests.remove(instance)

        return Response(status=status.HTTP_204_NO_CONTENT)


class UserInterests(generics.ListCreateAPIView):
    queryset = Interest.objects.all()
    serializer_class = serializers.UserInterestsSerializer
    lookup_field = 'username'
    permission_classes = (permissions.IsAuthenticated, IsAdminOrIsSelf,)

    def get_queryset(self):
        # self.kwargs.get('username'), returns lookup_field value
        user = User.objects.get(username=self.kwargs.get('username'))
        return user.interests.all()


class UserPosts(generics.ListAPIView):
    queryset = Post.objects.all()
    serializer_class = serializers.PostSerializer
    lookup_field = 'username'
    permission_classes = (permissions.IsAuthenticated, IsAdminOrIsSelf,)

    def get_queryset(self):
        # self.kwargs.get('username'), returns lookup_field value
        user = User.objects.get(username=self.kwargs.get('username'))
        return user.posts.all()


class UserViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = User.objects.all()
    serializer_class = serializers.UserSerializer
    lookup_field = 'username'
    permission_classes = (permissions.IsAuthenticated, IsAdminOrIsSelf,)

    def get_queryset(self):
        user = self.request.user
        if user.is_staff:
            return User.objects.all()
        else:
            return User.objects.filter(username=user.username)


class PostViewSet(mixins.ListModelMixin,
                  mixins.CreateModelMixin,
                  mixins.RetrieveModelMixin,
                  mixins.DestroyModelMixin,
                  viewsets.GenericViewSet):
    queryset = Post.objects.all()
    serializer_class = serializers.PostSerializer
    permission_classes = (permissions.IsAuthenticated, IsAdminOrIsSelf,)

    def list(self, request, *args, **kwargs):
        queryset = self.get_queryset()
        if not queryset.exists():
            return Response([], status=status.HTTP_204_NO_CONTENT)

        timestamp_in_locations = queryset.latest('creation_date').creation_date
        try:
            timestamp = validators.timestamp_query_param_validator(self.request.query_params)
        except exceptions.ParseError:
            pass
        else:
            if timestamp_in_locations == timestamp:
                return Response([], status=status.HTTP_204_NO_CONTENT)
            queryset = queryset.filter(creation_date__gt=timestamp)

        serializer = self.get_serializer(queryset, many=True)
        result = [{'result': [item for item in serializer.data]}]

        # Add last update timestamp
        unix_timestamp = float(timestamp_in_locations.strftime('%s.%f'))
        result.insert(0, OrderedDict([('timestamp', unix_timestamp)]))

        return Response(result, status=status.HTTP_200_OK)

    def get_queryset(self):
        if self.action == 'list':
            query_params = self.request.query_params
            if query_params:
                valid_location_ids = validators.location_posts_query_param_validator(query_params)
                if valid_location_ids:
                    return self.valid_posts(valid_location_ids)
            raise exceptions.MethodNotAllowed(
                'Cannot perform GET without query params. See documentation for more information')
        return self.queryset

    def valid_posts(self, location_ids):

        active_location_posts = self.queryset \
            .filter(start_date__lte=timezone.now(), end_date__gte=timezone.now()) \
            .filter(location__in=location_ids)

        wl_for_all = active_location_posts.filter(policy='WL', interests__isnull=True)

        user_interests = self.request.user.interests.all()
        valid_wl_posts = active_location_posts \
            .filter(policy='WL') \
            .filter(interests__in=user_interests)
        valid_bl_posts = active_location_posts \
            .filter(policy='BL') \
            .exclude(interests__isnull=True) \
            .exclude(interests__in=user_interests)

        return wl_for_all | valid_wl_posts | valid_bl_posts


class LocationViewSet(ListAddTimestampMixin,
                      mixins.CreateModelMixin,
                      mixins.RetrieveModelMixin,
                      mixins.DestroyModelMixin,
                      viewsets.GenericViewSet):
    queryset = Location.objects.all()
    serializer_class = serializers.LocationSerializer
    permission_classes = (permissions.IsAuthenticated,)

    def get_queryset(self):
        query_params = self.request.query_params
        if query_params:
            timestamp = validators.timestamp_query_param_validator(self.request.query_params)
            return self.queryset.filter(creation_date__gt=timestamp)
        return self.queryset.all()

    def destroy(self, request, *args, **kwargs):
        instance = self.get_object()

        if instance.posts.filter(end_date__gt=timezone.now()).exists():
            raise FailedDependency(detail='Unable to remove Location because there are valid Post(s) associated')

        return super(LocationViewSet, self).destroy(request, *args, **kwargs)
