import { useEffect, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import axiosInstance from '../api/axiosInstance';

export default function AiAutofillModal({ onApply, onClose }) {
  const [selectedImage, setSelectedImage] = useState(null);
  const [selectedImageUrl, setSelectedImageUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [cameraError, setCameraError] = useState('');
  const [isCameraRunning, setIsCameraRunning] = useState(false);
  const videoRef = useRef(null);
  const streamRef = useRef(null);

  function stopCamera() {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => {
        try {
          track.stop();
        } catch {
          // ignore track stop errors
        }
      });
      streamRef.current = null;
    }

    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }

    setIsCameraRunning(false);
  }

  async function startCamera() {
    setCameraError('');

    try {
      stopCamera();
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false,
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }

      setIsCameraRunning(true);
    } catch (error) {
      setCameraError(error?.message || 'Unable to access camera');
      setIsCameraRunning(false);
    }
  }

  function captureFromCamera() {
    const video = videoRef.current;
    if (!video || !video.videoWidth || !video.videoHeight) {
      toast.error('Camera is not ready yet. Please wait and retry.');
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const context = canvas.getContext('2d');
    if (!context) {
      toast.error('Could not capture image from camera');
      return;
    }

    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          toast.error('Could not capture image from camera');
          return;
        }

        const capturedFile = new File([blob], `capture-${Date.now()}.jpg`, { type: 'image/jpeg' });
        setSelectedImage(capturedFile);
        setPreviewData(null);
        stopCamera();
      },
      'image/jpeg',
      0.92
    );
  }

  useEffect(() => {
    if (!selectedImage) {
      setSelectedImageUrl('');
      return;
    }

    const objectUrl = URL.createObjectURL(selectedImage);
    setSelectedImageUrl(objectUrl);

    return () => URL.revokeObjectURL(objectUrl);
  }, [selectedImage]);

  useEffect(() => {
    return () => {
      stopCamera();
    };
  }, []);

  async function uploadAndAutofill() {
    if (!selectedImage) {
      toast.error('Please upload or capture a photo first');
      return;
    }

    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('image', selectedImage);
      const response = await axiosInstance.post('/api/ai/autofill', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      const data = response.data;
      setPreviewData(data);
      if (!data.success) {
        toast.error(data.message || 'Autofill could not extract details');
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Autofill request failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/70 p-4">
      <div className="mx-auto my-6 w-full max-w-2xl rounded-xl border border-slate-700 bg-slate-900 p-4 text-slate-100 md:my-10 max-h-[calc(100vh-3rem)] overflow-y-auto">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-lg font-semibold">AI Photo Autofill</h3>
          <button
            type="button"
            className="rounded border border-slate-600 px-2 py-1 text-sm"
            onClick={() => {
              stopCamera();
              onClose();
            }}
          >
            Close
          </button>
        </div>

        <p className="mb-3 text-sm text-slate-300">Upload a photo or capture live from camera, then let AI fill medicine details.</p>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded border border-slate-700 bg-slate-950 p-3">
            <p className="mb-2 text-xs text-slate-400">Upload Photo</p>
            <input
              type="file"
              accept="image/*"
              onChange={(event) => {
                const file = event.target.files?.[0] || null;
                setSelectedImage(file);
                setPreviewData(null);
              }}
              className="w-full rounded border border-slate-700 bg-slate-900 p-2 text-sm"
            />
          </div>

          <div className="rounded border border-slate-700 bg-slate-950 p-3">
            <p className="mb-2 text-xs text-slate-400">Live Camera Capture</p>
            <div className="overflow-hidden rounded border border-slate-700 bg-black">
              <video ref={videoRef} className="h-40 w-full object-cover" playsInline muted />
            </div>
            {cameraError ? <p className="mt-2 text-xs text-rose-400">{cameraError}</p> : null}
            <div className="mt-2 flex gap-2">
              <button
                type="button"
                className="rounded border border-cyan-500/60 px-3 py-1 text-xs text-cyan-100 hover:bg-cyan-500/20"
                onClick={startCamera}
                disabled={loading}
              >
                {isCameraRunning ? 'Restart Camera' : 'Start Camera'}
              </button>
              <button
                type="button"
                className="rounded border border-emerald-500/60 px-3 py-1 text-xs text-emerald-100 hover:bg-emerald-500/20 disabled:opacity-50"
                onClick={captureFromCamera}
                disabled={!isCameraRunning || loading}
              >
                Capture
              </button>
              <button
                type="button"
                className="rounded border border-slate-600 px-3 py-1 text-xs hover:bg-slate-800"
                onClick={stopCamera}
                disabled={!isCameraRunning}
              >
                Stop
              </button>
            </div>
          </div>
        </div>

        {selectedImageUrl ? (
          <div className="mt-4 rounded border border-slate-700 bg-slate-950 p-3">
            <p className="mb-2 text-xs text-slate-400">Selected Image Preview</p>
            <img src={selectedImageUrl} alt="Selected for AI autofill" className="max-h-56 w-full rounded object-contain" />
          </div>
        ) : null}

        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium hover:bg-blue-500 disabled:opacity-50"
            onClick={uploadAndAutofill}
            disabled={loading}
          >
            {loading ? 'Analyzing...' : 'Analyze Photo'}
          </button>
          <button
            type="button"
            className="rounded-md border border-slate-600 px-4 py-2 text-sm hover:bg-slate-800"
            onClick={() => {
              setSelectedImage(null);
              setPreviewData(null);
            }}
            disabled={loading}
          >
            Clear Selection
          </button>
        </div>

        {previewData ? (
          <div className="mt-4 rounded border border-slate-700 bg-slate-950 p-3">
            <p className="mb-2 text-xs text-slate-400">Provider: {previewData.providerUsed || '-'}</p>
            <pre className="max-h-56 overflow-auto whitespace-pre-wrap text-xs text-slate-200">
              {JSON.stringify(previewData, null, 2)}
            </pre>
            <button
              type="button"
              className="mt-3 rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium hover:bg-emerald-500"
              onClick={() => onApply(previewData)}
            >
              Apply To Form
            </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}
