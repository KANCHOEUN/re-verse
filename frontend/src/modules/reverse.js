import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  // isOpen: false,
  info: {
    archiveId: "",
    stuffs: [],
  },
  travelWriteIsOpen: false,
  travelReadIsOpen: false,
  annivWriteIsOpen: false,
  annivReadIsOpen: false,
  diaryWriteIsOpen: false,
  diaryReadIsOpen: false,
  isCampfireOn: 0,
  article: {
    title: "",
    content: "",
    memoryDate: `${new Date()}`,
  },
  travel: {
    articleList: [],
    timelines: [],
  },
  anniv: {
    articleList: [],
    timelines: [],
  },
  diary: {
    articleList: [],
    timelines: [],
  },
};

const reducers = {
  setInfo: (state, action) => {
    state.info = action.payload;
  },
  setTravelWriteIsOpen: (state, action) => {
    state.travelWriteIsOpen = !state.travelWriteIsOpen;
  },
  setTravelReadIsOpen: (state, action) => {
    state.travelReadIsOpen = !state.travelReadIsOpen;
  },
  setAnnivWriteIsOpen: (state, action) => {
    state.annivWriteIsOpen = !state.annivWriteIsOpen;
  },
  setAnnivReadIsOpen: (state, action) => {
    state.annivReadIsOpen = !state.annivReadIsOpen;
  },
  setDiaryWriteIsOpen: (state, action) => {
    state.diaryWriteIsOpen = !state.diaryWriteIsOpen;
  },
  setDiaryReadIsOpen: (state, action) => {
    state.diaryReadIsOpen = !state.diaryReadIsOpen;
  },
  setCampfireOn: (state, action) => {
    state.isCampfireOn = action.payload;
  },
  createArticle: (state, action) => {
    state.article = action.payload;
  },
  setTravel: (state, action) => {
    state.travel = action.payload;
  },
  setAnniv: (state, action) => {
    state.anniv = action.payload;
  },
  setDiary: (state, action) => {
    state.diary = action.payload;
  },
};

export const reverseSlice = createSlice({
  name: "reverse",
  initialState,
  reducers,
});

export const {
  setInfo,
  setTravelWriteIsOpen,
  setTravelReadIsOpen,
  setAnnivWriteIsOpen,
  setAnnivReadIsOpen,
  setDiaryWriteIsOpen,
  setDiaryReadIsOpen,
  setCampfireOn,
  createArticle,
  setTravel,
  setAnniv,
  setDiary,
} = reverseSlice.actions;

export default reverseSlice.reducer;
