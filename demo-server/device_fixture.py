"""Local device test fixture. Never included in the deployment image."""
import os
from server import Config, DemoApp


def create_fixture():
    if os.environ.get("DEMO_SYNTHETIC_TEST") != "1":
        raise RuntimeError("Synthetic test fixture requires explicit opt-in")
    return DemoApp(Config(os.environ["DEMO_DATABASE"], "synthetic-unused-key"),
                   lambda config, message: ("Risposta demo sintetica", {"input_tokens": 10, "output_tokens": 5}))
