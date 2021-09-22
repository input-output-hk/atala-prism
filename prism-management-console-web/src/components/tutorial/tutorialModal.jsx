import React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'antd';
import TutorialCarrousel from './tutorialCarrousel/tutorialCarrousel';

import './_style.scss';

const TutorialModal = ({ isOpen, onStart, onPause }) => (
  <Modal className="modalTutorial" visible={isOpen} onCancel={onPause}>
    <TutorialCarrousel onStart={onStart} />
  </Modal>
);

TutorialModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onStart: PropTypes.func.isRequired,
  onPause: PropTypes.func.isRequired
};

export default TutorialModal;
