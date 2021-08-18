import React, { useState } from 'react';
import { Progress, Modal } from 'antd';
import success from '../../../images/tutSuccess.svg';
import './_style.scss';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

const ProgressChecklist = ({text, percent}) => {
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
    <a onClick={showModal} className="progressChecklist">
      {/* this should link to each page now it links to the complete section modal but it should only do this once completed the setp */}
      <Progress percent={percent} />
      <p>{text}</p>
    </a>

    <Modal className="tutorialSuccessModal" visible={isModalVisible} onOk={handleOk} onCancel={handleCancel}>
        <div>
          <img src={success} alt="tutorialMessage" />
        </div>
        <div className="modalText">
        <h2>Basic Steps</h2>
        <p>You have completed this unit</p>
        </div>
        <div className="buttonModal">
          <CustomButton buttonProps={{
          className: 'theme-outline'
        }}
        buttonText="Skip" />
          <CustomButton buttonProps={{
          className: 'theme-primary'
        }}
        buttonText="Continue" />
        </div>
      </Modal>

    </>
  );
};

export default ProgressChecklist;
