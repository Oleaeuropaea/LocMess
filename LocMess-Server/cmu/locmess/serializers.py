import math
from collections import OrderedDict

from django.contrib.auth.models import User
from django.utils import timezone
from rest_framework import serializers

from .models import Post, Interest, Location


class InterestNameSerializer(serializers.ModelSerializer):
    class Meta:
        model = Interest
        fields = ('name',)


class InterestNameValueSerializer(serializers.ModelSerializer):
    class Meta:
        model = Interest
        fields = ('name', 'value')


class InterestFullSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Interest
        fields = ('url', 'name', 'value')


class UserInterestsSerializer(InterestFullSerializer):
    def create(self, validated_data):
        obj, created = Interest.objects.get_or_create(**validated_data)

        user = self.context['request'].user
        user.interests.add(obj)

        return obj


class UserSerializer(serializers.HyperlinkedModelSerializer):
    url = serializers.HyperlinkedIdentityField(view_name='user-detail', lookup_field='username')
    interests = serializers.HyperlinkedIdentityField(view_name='user-interests-list', lookup_field='username')
    posts = serializers.HyperlinkedIdentityField(view_name='user-posts-list', lookup_field='username')

    class Meta:
        model = User
        fields = ('url', 'username', 'interests', 'posts')
        read_only_fields = ('username', 'posts',)


class LocationSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Location
        fields = ('url', 'id', 'type', 'name', 'latitude', 'longitude', 'radius', 'ssid')
        extra_kwargs = {
            'radius': {'required': False},
            'ssid': {'required': False},
        }

    def create(self, validated_data):
        if validated_data.get('type') == 'GPS':
            validated_data['latitude'] = math.radians(float(validated_data['latitude']))
            validated_data['longitude'] = math.radians(float(validated_data['longitude']))

        return super(LocationSerializer, self).create(validated_data)

    def validate(self, data):
        if data.get('type') == 'GPS':
            if not data.get('latitude') or not data.get('longitude') or not data.get('radius'):
                raise serializers.ValidationError("(latitude, longitude, radius) must be provide for GPS location")
            if data.get('ssid'):
                raise serializers.ValidationError("(ssid) is not valid for GPS location")
        else:
            if not data.get('ssid'):
                raise serializers.ValidationError("(ssid) must be provide for WIFI/BLE location")
            if data.get('latitude') or data.get('longitude') or data.get('radius'):
                raise serializers.ValidationError("(latitude, longitude, radius) is not valid for WIFI/BLE location")
        return data

    # exclude blank fields
    def to_representation(self, instance):
        if instance.type == 'GPS':
            # Convert latitude and longitude back to degrees
            instance.latitude = math.degrees(float(instance.latitude))
            instance.longitude = math.degrees(float(instance.longitude))

        ret = super(LocationSerializer, self).to_representation(instance)
        ret = OrderedDict(list(filter(lambda x: x[1], ret.items())))
        return ret


class PostSerializer(serializers.HyperlinkedModelSerializer):
    location = LocationSerializer(read_only=True)
    location_url = serializers.HyperlinkedRelatedField(
        view_name='location-detail', write_only=True, source='location', queryset=Location.objects.all()
    )
    owner = serializers.HiddenField(default=serializers.CurrentUserDefault())
    sender = serializers.StringRelatedField(source='owner.username')
    sender_email = serializers.StringRelatedField(source='owner.email')
    interests = InterestNameValueSerializer(many=True)
    centralized_mode = serializers.BooleanField(default=True, read_only=True)

    class Meta:
        model = Post
        fields = (
            'url', 'subject', 'location', 'location_url', 'centralized_mode', 'creation_date', 'owner',
            'sender', 'sender_email', 'policy', 'interests', 'start_date', 'end_date', 'content'
        )
        read_only_fields = ('creation_date',)

    def validate_start_date(self, value):
        if value < timezone.now() - timezone.timedelta(minutes=5):
            raise serializers.ValidationError('StartDate must not be set in the past')
        return value

    def validate(self, data):
        if data.get('start_date') > data.get('end_date'):
            raise serializers.ValidationError('start_date must be leaser then end_date')
        return data

    def create(self, validated_data):
        interests = validated_data.pop('interests')
        instance = Post.objects.create(**validated_data)

        for interest in interests:
            interest['name'] = interest['name'].lower()
            interest['value'] = interest['value'].lower()

            obj, created = Interest.objects.get_or_create(**interest)
            instance.interests.add(obj)

        return instance

    def to_representation(self, instance):
        ret = super(PostSerializer, self).to_representation(instance)
        if instance.owner != self.context['request'].user:
            ret.pop('policy')
            ret.pop('end_date')
            ret.pop('interests')
        return ret
