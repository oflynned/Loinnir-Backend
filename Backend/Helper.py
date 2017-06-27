import json


class Helper:
    @staticmethod
    def get_path(mode):
        if mode == "prod":
            return "../../loinnir_auth.json"
        else:
            return "../../../../loinnir_auth.json"

    @staticmethod
    def get_fcm_api_key(mode):
        with open(Helper.get_path(mode), "r") as f:
            data = json.loads(f.read())
            return data["fcm_api_key"]

    @staticmethod
    def get_places_api_key(mode):
        with open(Helper.get_path(mode), "r") as f:
            data = json.loads(f.read())
            return data["places_api_key"]

    @staticmethod
    def get_populated_areas():
        with open("./populated_areas.json", "r") as f:
            data = json.loads(f.read())
            return data["features"]

    @staticmethod
    def get_groomed_populated_areas():
        with open("./groomed_populated_areas_localised.json", "r") as f:
            return json.loads(f.read())

    @staticmethod
    def generate_fake_users():
        for i in range(100):
            area = ""
            name = ""
            lat = ""
            lng = ""
            profile_pic = ""

            # get random town from json
            # add/subtract random value on lat & lng up to 10km
            # regenerate locality for the fake profile
