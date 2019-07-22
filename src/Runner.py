from src.core import *
from src.utils import *


class Runner(Exceptionable, Configurable):

    def __init__(self, config_file_path: str):

        # initialize Configurable super class
        Configurable.__init__(self, SetupMode.NEW, ConfigKey.MASTER, config_file_path)

        # get config path info from config and set to class vars
        self.exceptions_config_path = self.path(ConfigKey.MASTER, 'config_paths', 'exceptions')

        # initialize Exceptionable super class
        Exceptionable.__init__(self, SetupMode.NEW, self.exceptions_config_path)

    def run(self):
        _ = SlideMap(self.configs[ConfigKey.MASTER.value],
                     self.configs[ConfigKey.EXCEPTIONS.value])

        self.trace = Trace([0], [0], [0],
                           self.configs[ConfigKey.MASTER.value],
                           self.configs[ConfigKey.EXCEPTIONS.value])
        # TEST: exceptions configuration path
        # print('exceptions_config_path:\t{}'.format(self.exceptions_config_path))

        # TEST: retrieve data from config file
        # print(self.search(ConfigKey.MASTER, 'test_array', 0, 'test'))

        # TEST: throw error
        self.throw(2)
