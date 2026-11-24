"""Hot air balloons over Cappadocia (Turkey): 3D shapes on the WorldWind globe of Bio7.

The balloons climb to their cruising altitude and drift with the wind, which turns and becomes
stronger with the altitude. In Bio7 they are OpenGL spheres in their altitude (no ImageJ), in
the exported page points on an online map.
"""
from bio7sim import GeoAgentModel, GeoAgent, param, action, simulation

START_LON, START_LAT = 34.829, 38.643
COLORS = ["#e63946", "#f4a261", "#e9c46a", "#2a9d8f", "#457b9d", "#9b5de5", "#f15bb5", "#00bbf9"]


class Balloon(GeoAgent):
    """A balloon: its cruising altitude, the minutes in the air, landed."""

    def __init__(self, lon, lat, cruise):
        super().__init__(lon, lat)
        self.cruise = cruise
        self.minutes = 0
        self.landed = False


class HotAirBalloons(GeoAgentModel):
    """Hot air balloons over Cappadocia: they climb to their cruising altitude and drift with the wind, which turns and becomes stronger with the altitude."""

    balloons = param(60, min=5, max=150, label="Number of balloons", restart=True, group="Balloons")
    max_altitude = param(1200.0, min=200, max=2000, step=50, label="Maximum cruising altitude (m)", restart=True, group="Balloons")
    flight_time = param(60, min=20, max=120, label="Flight time (minutes)", group="Balloons")
    wind_from = param(40.0, min=0, max=360, step=5, label="Wind from at the ground (degrees, 0 north)", group="Wind")
    wind_speed = param(2.0, min=0, max=8, step=0.1, label="Wind speed at the ground (m/s)", group="Wind")
    veering = param(60.0, min=-120, max=120, step=5, label="Turning of the wind (degrees per 1000 m)", group="Wind")

    def init(self):
        self.area(38.50, 34.65, 38.80, 35.05)        # south, west, north, east
        self.agent_shape = "sphere"                  # OpenGL 3D shapes on the globe (point, sphere, cone, box, cylinder)
        self.agent_size_meters = 120
        self.view_pitch = 65                         # an oblique view
        self.agent_size = 5                          # the size of the points on the map (pixels)
        for i in range(self.balloons):
            self.agents.append(self.new_balloon(COLORS[i % len(COLORS)], -self.random_int(15)))   # the start is spread over 15 minutes

    def new_balloon(self, color, minutes=0):
        b = Balloon(START_LON + self.random(-0.03, 0.03), START_LAT + self.random(-0.02, 0.02), self.random(150, self.max_altitude))
        b.color, b.minutes = color, minutes
        return b

    def step(self):
        flying, altitudes = 0, 0.0
        for b in self.agents:
            b.minutes += 1
            if b.landed or b.minutes < 0:
                continue
            # climb to the cruising altitude (2 m/s), descend at the end of the flight
            if b.minutes > self.flight_time:
                b.altitude = max(0.0, b.altitude - 90)
                if b.altitude == 0:
                    b.landed, b.color = True, "#8d8d8d"
                    continue
            elif b.altitude < b.cruise:
                b.altitude = min(b.cruise, b.altitude + 120)
            # the wind in the altitude of the balloon: it turns and becomes stronger
            wind_from = self.wind_from + self.veering * b.altitude / 1000
            speed = self.wind_speed * (1 + b.altitude / 800) + self.random_gaussian(0, 0.3)
            km = speed * 60 / 1000
            # the wind blows from wind_from: the movement is in the opposite direction
            self.move(b, -self.sin_deg(wind_from) * km, -self.cos_deg(wind_from) * km)
            flying += 1
            altitudes += b.altitude
        self.chart("Balloons in the air", flying)
        self.chart("Mean altitude (m)", altitudes / flying if flying else 0)

    @action("Start a new group of balloons")
    def new_group(self):
        for _ in range(10):
            self.agents.append(self.new_balloon(self.random_element(COLORS)))


simulation(HotAirBalloons)
