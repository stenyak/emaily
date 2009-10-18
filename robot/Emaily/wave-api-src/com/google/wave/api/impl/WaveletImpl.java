/* Copyright (c) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.wave.api.impl;

import com.google.wave.api.Blip;
import com.google.wave.api.StyledText;
import com.google.wave.api.Wavelet;

import java.util.List;
import java.util.Map;

/**
 * The {@link Wavelet} implementation wraps a {@link WaveletData} object and is
 * used to traverse blips and other metadata if present in the message bundle.
 * 
 * @author scovitz@google.com (Seth Covitz)
 */
public class WaveletImpl implements Wavelet {
  
  private RobotMessageBundleImpl events;
  private WaveletData waveletData;
  
  public WaveletImpl(WaveletData waveletData, RobotMessageBundleImpl events) {
    this.waveletData = waveletData;
    this.events = events;
  }

  public String getWaveId() {
    return waveletData.getWaveId();
  }
  
  public String getWaveletId() {
    return waveletData.getWaveletId();
  }

  public Blip getRootBlip() {
    return new BlipImpl(events.getBlipData().get(waveletData.getRootBlipId()), events);
  }
  
  public String getRootBlipId() {
    return waveletData.getRootBlipId();
  }

  
  public Blip appendBlip() {
    BlipData blipData = new BlipData();
    blipData.setWaveId(getWaveId());
    blipData.setWaveletId(getWaveletId());
    blipData.setBlipId("TBD" + Math.random());
    events.addOperation(new OperationImpl(OperationType.WAVELET_APPEND_BLIP, getWaveId(),
        getWaveletId(), null, -1, blipData));
    return new BlipImpl(blipData, events);
  }

  
  public Blip appendBlip(String writeBackDataDocument) {
    BlipData blipData = new BlipData();
    blipData.setWaveId(getWaveId());
    blipData.setWaveletId(getWaveletId());
    blipData.setBlipId("TBD" + Math.random());
    events.addOperation(new OperationImpl(OperationType.WAVELET_APPEND_BLIP, getWaveId(),
        getWaveletId(), writeBackDataDocument, -1, blipData));
    return new BlipImpl(blipData, events);
  }

  
  public void setTitle(String title) {
    waveletData.setTitle(title);
    events.addOperation(new OperationImpl(OperationType.WAVELET_SET_TITLE,
        getWaveId(), getWaveletId(), null, -1, title));
  }
  
  
  public String getTitle() {
    return waveletData.getTitle();
  }

  public String getDataDocument(String name) {
    return waveletData.getDataDocument(name);
  }

  public void addParticipant(String participant) {
    events.addOperation(new OperationImpl(OperationType.WAVELET_ADD_PARTICIPANT,
        getWaveId(), getWaveletId(), null, -1, participant));
    waveletData.getParticipants().add(participant);
  }

  
  public List<String> getParticipants() {
    return waveletData.getParticipants();
  }

  
  public long getCreationTime() {
    return waveletData.getCreationTime();
  }

  
  public long getLastModifiedTime() {
    return waveletData.getLastModifiedTime();
  }

  
  public long getVersion() {
    return waveletData.getVersion();
  }

  
  public void setDataDocument(String name, String data) {
    events.addOperation(new OperationImpl(OperationType.WAVELET_DATADOC_SET,
        getWaveId(), getWaveletId(), name, -1, data));
    waveletData.setDataDocument(name, data);
  }

  
  public Map<String, String> getDataDocuments() {
    return waveletData.getDataDocuments();
  }

  
  public boolean hasDataDocument(String name) {
    return waveletData.getDataDocuments() == null ? false :
        waveletData.getDataDocuments().containsKey(name);
  }

  
  public void setTitle(StyledText styledText) {
    waveletData.setTitle(styledText.getText());
    events.addOperation(new OperationImpl(OperationType.WAVELET_SET_TITLE,
        getWaveId(), getWaveletId(), null, styledText.getStyles().get(0).ordinal(),
        styledText.getText()));
  }

  
  public void appendDataDocument(String name, String data) {
    events.addOperation(new OperationImpl(OperationType.WAVELET_DATADOC_APPEND,
        getWaveId(), getWaveletId(), name, -1, data));
    if (waveletData.getDataDocument(name) == null) {
      waveletData.setDataDocument(name, data);
    } else {
      waveletData.setDataDocument(name, waveletData.getDataDocument(name).concat(data));
    }
  }

  
  public String getCreator() {
    return waveletData.getCreator();
  }

  
  public void removeParticipant(String participant) {
    events.addOperation(new OperationImpl(OperationType.WAVELET_REMOVE_PARTICIPANT,
        getWaveId(), getWaveletId(), null, -1, participant));
  }
  
  
  public Wavelet createWavelet(List<String> participants, String annotationWriteBack) {
    WaveletData waveletData = new WaveletData();
    waveletData.setWaveId("TBD" + Math.random());
    waveletData.setWaveletId("conv+root");
    waveletData.setParticipants(participants);
    BlipData blipData = new BlipData();
    blipData.setBlipId("TBD" + Math.random());
    blipData.setWaveId(waveletData.getWaveId());
    blipData.setWaveletId(waveletData.getWaveletId());
    events.getBlipData().put(blipData.getBlipId(), blipData);
    waveletData.setRootBlipId(blipData.getBlipId());
    
    events.addOperation(new OperationImpl(OperationType.WAVELET_CREATE, this.getWaveId(),
        this.getWaveletId(), annotationWriteBack, -1, waveletData));
    
    return new WaveletImpl(waveletData, events);
  }
}
