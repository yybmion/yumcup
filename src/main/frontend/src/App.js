import React, {useEffect, useState} from 'react';
import axios from 'axios';

function App() {
  const [msg, setMsg] = useState('')

  useEffect(() => {
    axios.get('/hello')
        .then(response => setMsg(response.data))
        .catch(error => console.log(error))
  }, []);

  return (
      <div>
        백엔드 통신 성공? : {msg}
      </div>
  );
}

export default App;