from pathlib import Path
from subprocess import CompletedProcess
import tempfile
import unittest
from unittest.mock import patch

import capture_protocol_evidence as evidence


class CaptureTest(unittest.TestCase):
    def test_allowlist_rejects_addresses_arbitrary_messages_and_oversized_data(self):
        good = "PODS_TRACE t=123 event=rx kind=control id=06 size=11 data=01 00 00 00"
        short = "PODS_TRACE t=124 event=rx kind=head size=20 data=" + " ".join(["00"] * 20)
        self.assertEqual([good, short], evidence.records("\n".join([
            good, short, short + " 00", good + " AA:BB:CC:DD:EE:FF",
            good.replace("id=06", "id=1F"), "unrelated private log",
        ])))

    def test_output_inside_repository_and_invalid_duration_are_rejected(self):
        with self.assertRaises(ValueError):
            evidence.capture("adb", "test", evidence.ROOT / "evidence.log", 1)
        with self.assertRaises(ValueError):
            evidence.capture("adb", "test", Path("unused"), 121)

    def test_capture_is_read_only_deduplicated_and_time_bounded(self):
        now = [0.0]
        record = "PODS_TRACE t=1 event=begin reason=ownership"
        def sleep(seconds):
            now[0] += seconds
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(evidence.time, "monotonic", side_effect=lambda: now[0]), \
                patch.object(evidence.time, "sleep", side_effect=sleep), \
                patch.object(evidence.subprocess, "run",
                             return_value=CompletedProcess([], 0, record + "\n" + record)) as run:
            path = Path(directory) / "capture.log"
            self.assertEqual(1, evidence.capture("adb", "test", path, 2))
            self.assertEqual(record + "\n", path.read_text())
            self.assertEqual(2, now[0])
            for call in run.call_args_list:
                self.assertEqual(["adb", "-s", "test", "shell", evidence.COMMAND], call.args[0])
                self.assertLessEqual(call.kwargs["timeout"], 2)
            with self.assertRaises(FileExistsError):
                evidence.capture("adb", "test", path, 1)

    def test_missing_records_is_an_explicit_failure(self):
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(evidence.time, "monotonic", side_effect=[0, 0, 1, 1]), \
                patch.object(evidence.time, "sleep"), \
                patch.object(evidence.subprocess, "run", return_value=CompletedProcess([], 0, "")):
            with self.assertRaisesRegex(RuntimeError, "No matching evidence"):
                evidence.capture("adb", "test", Path(directory) / "empty.log", 1)


if __name__ == "__main__":
    unittest.main()
