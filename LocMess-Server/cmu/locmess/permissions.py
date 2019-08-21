from django.contrib.auth.models import User

from rest_framework import permissions

from .models import Interest


class IsAdminOrIsSelf(permissions.BasePermission):
    def has_object_permission(self, request, view, obj):
        if isinstance(obj, User):
            obj_owner = request.user
        elif isinstance(obj, Interest):
            if request.user.interests.filter(pk=obj.pk).exists():  # User interest
                obj_owner = request.user
            else:
                return False  # Post interest
        else:
            obj_owner = obj.owner

        return obj_owner == request.user or request.user.is_staff
