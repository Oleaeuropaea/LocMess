from rest_framework.exceptions import APIException


class FailedDependency(APIException):
    status_code = 424
    default_detail = 'This action depend on another.'
    default_code = 'failed_dependency'
