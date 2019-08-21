from django.utils import timezone

from rest_framework.exceptions import ParseError


def location_posts_query_param_validator(query_params):
    location_ids = query_params.getlist('location_id', None)
    if location_ids is not None:
        for location_id in location_ids:
            try:
                int(location_id)
            except ValueError:
                raise ParseError('location_id must be a int')
        return location_ids
    raise ParseError('Invalid query parameters. Options are: location')


def timestamp_query_param_validator(query_params):
    timestamp = query_params.get('timestamp', None)
    if timestamp is not None:
        try:
            timestamp = float(timestamp)
        except ValueError:
            raise ParseError('timestamp must be a float')

        return timezone.datetime.fromtimestamp(float(timestamp), tz=timezone.utc)
    raise ParseError('Invalid query parameters. Options are: timestamp')
