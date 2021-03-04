import { useState, useEffect } from 'react';
import { MAIN_CONTAINER_CLASS } from '../helpers/constants';

export const useScrolledToBottom = (toBeListened, containerClass = MAIN_CONTAINER_CLASS) => {
  const [timesScrolledToBottom, setTimesScrolledToBottom] = useState(0);

  useEffect(() => {
    const Container = document.querySelector(`.${containerClass}`);

    const scrollListener = ({ target: { scrollHeight, clientHeight, scrollTop } }) => {
      const maxScroll = scrollHeight - clientHeight;
      if (scrollTop === maxScroll) setTimesScrolledToBottom(timesScrolledToBottom + 1);
    };

    Container.addEventListener('scroll', scrollListener);

    return () => Container.removeEventListener('scroll', scrollListener);
  }, [...toBeListened]);
  // toBeListened[] is necessary to retrigger effect hook
  // Other way the hook is triggered only first time

  return { timesScrolledToBottom };
};
