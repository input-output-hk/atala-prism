import React, { useState } from 'react';
import { Modal, Button } from 'antd';
import TutorialCarrousel from './tutorialCarrousel/tutorialCarrousel.jsx';
import './_style.scss';

const TutorialModal = () => {
  const [isModalVisible, setIsModalVisible] = useState(false);

  const showModal = () => {
    setIsModalVisible(true);
  };

  const handleOk = () => {
    setIsModalVisible(false);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };
  return (
    <>
      <Button type="primary" onClick={showModal}>
        Open Modal
      </Button>
      <Modal className="modalTutorial" visible={isModalVisible} onOk={handleOk} onCancel={handleCancel}>
        <TutorialCarrousel />
      </Modal>
    </>
  );
};

export default TutorialModal;
