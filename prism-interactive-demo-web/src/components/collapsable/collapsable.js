import React from 'react';
import { Collapse } from 'antd';

import './_style.scss';

const { Panel } = Collapse;

const FAQs = [
  {
    title: 'How can I register for the Program?',
    content: [
      'If you are interested in joining the Atala PRISM Pioneer Program, please complete the registration form. If you are selected to join the Program, we will be in touch when the commencement dates for the course are confirmed.'
    ]
  },
  {
    title: 'When will the course start?',
    content: ['We aim to have the first course start in Q4 2021.']
  },
  {
    title: 'What prior experience do I need?',
    content: [
      'To participate in the first cohort of the Atala PRISM Pioneer Program, you must be familiar with Kotlin. Subsequent cohorts will be required to have experience with Java.'
    ]
  },
  {
    title: 'What can I expect to learn?',
    content: [
      'The Atala PRISM Pioneer Program offers a broad range of subjects and concepts. You will learn about the following:',
      '• 	How to use the SDK to create, sign, and issue verifiable credentials',
      '• 	How to use the SDK to create and publish decentralized Identifiers (DIDs)'
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
      'Upon completion of the course, Pioneers will be eligible to receive a certificate. In order to be eligible you must:',
      '1. 	Have registered for the program using the registration form',
      '2. 	Join the Discord group and have participated'
    ]
  },
  {
    title: 'What will be the certificate format?',
    content: [
      'You will receive an NFT that will be a class photo (from a Zoom call). In future, we aim to issue certificates via the Atala PRISM app (issued by IOG).'
    ]
  },
  {
    title: 'How will I receive my certificate?',
    content: [
      'You will receive instructions upon course completion on how to receive your certificate.'
    ]
  },
  {
    title:
      'Are there any funding opportunities for solutions built as part of the Atala PRISM Pioneer program?',
    content: [
      'Project Catalyst offers funding opportunities for the best solution ideas that can drive mass-scale adoption of DIDs on Cardano.',
      <>
        {'Everyone is invited to submit their proposal in '}
        <a href="https://cardano.ideascale.com/">
          Project Catalyst’s DID Mass-Scale Adoption Challenge and apply for funding.
        </a>
      </>
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
