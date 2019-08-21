from django.shortcuts import redirect
from django.urls import reverse

from .views_api import api_root


def index(request):
    return redirect(reverse(api_root))
