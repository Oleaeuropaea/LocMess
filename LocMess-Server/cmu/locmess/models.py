from django.db import models
from django.contrib.auth.models import User


class Location(models.Model):
    GPS = 'GPS'
    WIFI = 'WIFI'
    BEACON = 'BLE'
    LOCATION_CHOICES = (
        (GPS, 'GPS'),
        (WIFI, 'WIFI'),
        (BEACON, 'BLE')
    )

    type = models.CharField(max_length=4, choices=LOCATION_CHOICES, default=GPS)
    name = models.CharField(max_length=70)
    creation_date = models.DateTimeField(auto_now_add=True)

    # GPS
    latitude = models.FloatField(null=True, blank=True)
    longitude = models.FloatField(null=True, blank=True)
    radius = models.FloatField(null=True, blank=True)

    # WIFI
    ssid = models.CharField(max_length=70, blank=True)

    class Meta:
        ordering = ('creation_date',)

    def __str__(self):
        return self.name


class Post(models.Model):
    WHITE_LIST = 'WL'
    BLACK_LIST = 'BL'
    POLICY_CHOICES = (
        (WHITE_LIST, 'White List'),
        (BLACK_LIST, 'Black List'),
    )

    subject = models.CharField(max_length=70)
    location = models.ForeignKey(Location, on_delete=models.CASCADE, related_name='posts')
    creation_date = models.DateTimeField(auto_now_add=True)
    owner = models.ForeignKey(User, on_delete=models.CASCADE, related_name='posts')
    policy = models.CharField(max_length=2, choices=POLICY_CHOICES, default=WHITE_LIST)
    start_date = models.DateTimeField()
    end_date = models.DateTimeField()
    centralized_mode = models.BooleanField()
    content = models.TextField()

    class Meta:
        ordering = ('creation_date',)

    def __str__(self):
        return '{}: {}'.format(self.subject, self.owner.username)


class Interest(models.Model):
    name = models.CharField(max_length=35)
    value = models.CharField(max_length=70)
    creation_date = models.DateTimeField(auto_now_add=True)

    users = models.ManyToManyField(User, blank=True, related_name='interests')
    posts = models.ManyToManyField(Post, blank=True, related_name='interests')

    class Meta:
        ordering = ('creation_date',)

    def __str__(self):
        return '{}: {}'.format(self.name, self.value)
