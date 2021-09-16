import React from 'react';
import { Collapse } from 'antd';

import './_style.scss';

const { Panel } = Collapse;

const FAQs = [
  {
    title: 'How can I register for the Program?',
    content: [
      'If you are interested in joining the Atala PRISM Pioneers Program, please complete the registration form. You will receive a short application form, and we will be in touch when the commencement dates for the course are confirmed.'
    ]
  },
  {
    title: 'When will the course start?',
    content: ['We aim to have the first course start in Q4 2021.']
  },
  {
    title: 'What prior experience do I need?',
    content: ['A basic experience of Java would be required.']
  },
  {
    title: 'What can I expect to learn?',
    content: [
      'The Atala PRISM Pioneers Program offers a broad range of subjects and concepts. You will learn about the following:',
      '• 	Creation of  Verifiable credentials',
      '• 	Creation of  Decentralised Identifiers (DIDs)'
    ]
  },
  {
    title: 'How much time will I need to dedicate to the course?',
    content: [
      'The course will be approximately 6 weeks in duration. Each week’s lecture will be 1-2 hours long with 1 hour for Q&A. Additional coursework of between 1-8 hours will be required during the week depending on your skill level and availability. Of course, just like any other learning project, the more effort and dedication you put into it, the more you will get out of it.'
    ]
  },
  {
    title: 'What is the criteria for certification?',
    content: [
      'Upon completion of the course pioneers will be eligible to receive a certificate. In order to be eligible you must:',
      '1. 	Have registered for the program using the registration form',
      '2. 	Join the discord group and have participated'
    ]
  },
  {
    title: 'What will be the certificate format?',
    content: [
      'You will receive an NFT that will be a class photo (from a zoom call).In future, we aim to issue certificates via the Atala PRISM app (issued by IOG).'
    ]
  },
  {
    title: 'How will I receive my certificate?',
    content: [
      'You will receive instructions upon course completion on how to receive your certificate.'
    ]
  }
];

const Collapsable = () => {
  return (
    <Collapse className="Collapsable" expandIconPosition={'right'} defaultActiveKey={['1']}>
      {FAQs.map(({ title, content }, index) => (
        <Panel header={title} key={`FAQ-${index}`}>
          {content.map(text => (
            <p>{text}</p>
          ))}
        </Panel>
      ))}
    </Collapse>
  );
};

export default Collapsable;
