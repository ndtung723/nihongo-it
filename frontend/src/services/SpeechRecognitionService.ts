import * as sdk from "microsoft-cognitiveservices-speech-sdk";

class SpeechRecognitionService {
  private speechConfig: sdk.SpeechConfig | null = null;
  private recognizer: sdk.SpeechRecognizer | null = null;
  private isInitialized = false;
  private isRecognizing = false;

  // Khởi tạo service với key và region
  public initialize(speechKey: string, serviceRegion: string): boolean {
    try {
      this.speechConfig = sdk.SpeechConfig.fromSubscription(
        speechKey,
        serviceRegion,
      );
      this.speechConfig.speechRecognitionLanguage = "ja-JP";
      this.isInitialized = true;
      return true;
    } catch {
      return false;
    }
  }

  // Bắt đầu nhận dạng giọng nói
  public startRecognition(
    onRecognizing: (text: string) => void,
    onRecognized: (text: string) => void,
    onError: (error: unknown) => void,
  ): void {
    if (!this.isInitialized || !this.speechConfig) {
      onError(new Error("Speech recognition service not initialized"));
      return;
    }

    // Đảm bảo dừng bất kỳ phiên nhận dạng nào đang chạy
    if (this.isRecognizing && this.recognizer) {
      this.stopRecognition().catch(() => {});
    }

    try {
      const audioConfig = sdk.AudioConfig.fromDefaultMicrophoneInput();
      this.recognizer = new sdk.SpeechRecognizer(
        this.speechConfig,
        audioConfig,
      );

      this.recognizer.recognizing = (_, e) => {
        if (e.result.reason === sdk.ResultReason.RecognizingSpeech) {
          onRecognizing(e.result.text);
        }
      };

      this.recognizer.recognized = (_, e) => {
        if (e.result.reason === sdk.ResultReason.RecognizedSpeech) {
          onRecognized(e.result.text);
        } else if (e.result.reason === sdk.ResultReason.NoMatch) {
          // Chỉ báo lỗi nếu đang trong quá trình nhận dạng
          if (this.isRecognizing) {
            onError("No speech could be recognized");
          }
        }
      };

      this.recognizer.canceled = (_, e) => {
        if (e.reason === sdk.CancellationReason.Error) {
          onError(`Error: ${e.errorCode} ${e.errorDetails}`);
        }
        // Dừng nhận dạng khi có lỗi hủy
        this.isRecognizing = false;
      };

      // Thêm xử lý sự kiện khi phiên làm việc kết thúc
      this.recognizer.sessionStopped = (_data: unknown, _extra: unknown) => {
        this.isRecognizing = false;
        this.cleanupRecognizer();
      };

      this.recognizer.startContinuousRecognitionAsync(
        () => {
          this.isRecognizing = true;
        },
        (error) => {
          onError(error);
          this.isRecognizing = false;
        },
      );
    } catch (error) {
      onError(error);
      this.isRecognizing = false;
    }
  }

  // Dừng nhận dạng giọng nói
  public stopRecognition(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.recognizer || !this.isRecognizing) {
        this.isRecognizing = false;
        this.cleanupRecognizer();
        resolve();
        return;
      }

      try {
        this.recognizer.stopContinuousRecognitionAsync(
          () => {
            this.isRecognizing = false;
            this.cleanupRecognizer();
            resolve();
          },
          (error) => {
            this.isRecognizing = false;
            this.cleanupRecognizer();
            reject(error);
          },
        );
      } catch (error) {
        this.isRecognizing = false;
        this.cleanupRecognizer();
        reject(error);
      }
    });
  }

  // Làm sạch và giải phóng tài nguyên của recognizer
  private cleanupRecognizer(): void {
    if (this.recognizer) {
      try {
        this.recognizer.close();
      } catch {}
      this.recognizer = null;
    }
  }

  // Kiểm tra xem service đã được khởi tạo chưa
  public isServiceInitialized(): boolean {
    return this.isInitialized;
  }

  // Kiểm tra xem service có đang nhận dạng giọng nói không
  public isCurrentlyRecognizing(): boolean {
    return this.isRecognizing;
  }
}

export default new SpeechRecognitionService();
