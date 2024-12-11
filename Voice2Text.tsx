import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  TouchableHighlight,
  StyleSheet,
} from "react-native";
import Voice, {
  SpeechRecognizedEvent,
  SpeechResultsEvent,
  SpeechErrorEvent,
  SpeechStartEvent,
  SpeechEndEvent,
  SpeechVolumeChangeEvent,
} from "@react-native-voice/voice";

type SpeechResult = {
  value: string[];
};

type SpeechError = {
  error: {
    code: string;
    message: string;
  };
};

const App: React.FC = () => {
  const [recognized, setRecognized] = useState<string>("");
  const [pitch, setPitch] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [end, setEnd] = useState<string>("");
  const [started, setStarted] = useState<string>("");
  const [results, setResults] = useState<string[]>([]);
  const [partialResults, setPartialResults] = useState<string[]>([]);

  useEffect(() => {
    Voice.onSpeechStart = (e: SpeechStartEvent) => {
      console.log("onSpeechStart: ", e);
      setStarted("√");
    };

    Voice.onSpeechRecognized = (e: SpeechRecognizedEvent) => {
      console.log("onSpeechRecognized: ", e);
      setRecognized("√");
    };

    Voice.onSpeechEnd = (e: SpeechEndEvent) => {
      console.log("onSpeechEnd: ", e);
      setEnd("√");
    };

    Voice.onSpeechError = (e: SpeechErrorEvent) => {
      console.log("onSpeechError: ", e);
      setError(JSON.stringify((e as SpeechError).error));
    };

    Voice.onSpeechResults = (e: SpeechResultsEvent) => {
      console.log("onSpeechResults: ", e);
      setResults(e.value);
    };

    Voice.onSpeechPartialResults = (e: SpeechResultsEvent) => {
      console.log("onSpeechPartialResults: ", e);
      setPartialResults(e.value);
    };

    Voice.onSpeechVolumeChanged = (e: SpeechVolumeChangeEvent) => {
      console.log("onSpeechVolumeChanged: ", e);
      setPitch(e.value);
    };

    return () => {
      Voice.destroy().then(Voice.removeAllListeners);
    };
  }, []);

  const startRecognizing = async () => {
    try {
      await Voice.start("en-US");
    } catch (e) {
      console.error(e);
    }
  };

  const stopRecognizing = async () => {
    try {
      await Voice.stop();
    } catch (e) {
      console.error(e);
    }
  };

  const cancelRecognizing = async () => {
    try {
      await Voice.cancel();
    } catch (e) {
      console.error(e);
    }
  };

  const destroyRecognizer = async () => {
    try {
      await Voice.destroy();
    } catch (e) {
      console.error(e);
    }
    resetStates();
  };

  const resetStates = () => {
    setRecognized("");
    setPitch("");
    setError("");
    setStarted("");
    setResults([]);
    setPartialResults([]);
    setEnd("");
  };

  return (
    <View style={styles.container}>
      <Text style={styles.welcome}>Welcome to React Native Voice!</Text>
      <Text style={styles.instructions}>Press the button and start speaking.</Text>
      <Text style={styles.stat}>{`Started: ${started}`}</Text>
      <Text style={styles.stat}>{`Recognized: ${recognized}`}</Text>
      <Text style={styles.stat}>{`Pitch: ${pitch}`}</Text>
      <Text style={styles.stat}>{`Error: ${error}`}</Text>
      <Text style={styles.stat}>Results</Text>
      {results.map((result, index) => (
        <Text key={`result-${index}`} style={styles.stat}>{result}</Text>
      ))}
      <Text style={styles.stat}>Partial Results</Text>
      {partialResults.map((result, index) => (
        <Text key={`partial-result-${index}`} style={styles.stat}>{result}</Text>
      ))}
      <Text style={styles.stat}>{`End: ${end}`}</Text>

      <TouchableHighlight onPress={startRecognizing}>
        <Text style={styles.action}>Start</Text>
      </TouchableHighlight>
      <TouchableHighlight onPress={stopRecognizing}>
        <Text style={styles.action}>Stop Recognizing</Text>
      </TouchableHighlight>
      <TouchableHighlight onPress={cancelRecognizing}>
        <Text style={styles.action}>Cancel</Text>
      </TouchableHighlight>
      <TouchableHighlight onPress={destroyRecognizer}>
        <Text style={styles.action}>Destroy</Text>
      </TouchableHighlight>
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#F5FCFF",
    marginTop: 33,
  },
  welcome: {
    fontSize: 20,
    textAlign: "center",
    margin: 10,
  },
  action: {
    textAlign: "center",
    color: "#0000FF",
    marginVertical: 5,
    fontWeight: "bold",
  },
  instructions: {
    textAlign: "center",
    color: "#333333",
    marginBottom: 5,
  },
  stat: {
    textAlign: "center",
    color: "#B0171F",
    marginBottom: 1,
  },
});